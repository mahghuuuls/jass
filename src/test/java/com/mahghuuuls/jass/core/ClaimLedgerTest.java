package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimLedgerTest {

    private static final double EPS = 1e-9;

    @Test
    void noClaimsMeansAvailableIsStamina() {
        ClaimLedger ledger = new ClaimLedger();
        assertEquals(30.0, ledger.available(30.0), EPS);
        assertEquals(0.0, ledger.reserved(), EPS);
        assertTrue(ledger.isEmpty());
    }

    @Test
    void aDiscreteClaimMakesOtherActionsSeeItAsSpent() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim claim = ledger.register(false, 12.0);
        assertEquals(-11.0, ledger.available(1.0), EPS);
        assertEquals(1.0, ledger.availableExcept(1.0, claim), EPS);
        assertEquals(0.0, ledger.reserved(), EPS);
    }

    @Test
    void reconcileRepricesAnOpenClaimOnly() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim claim = ledger.register(false, 12.0);
        ledger.reconcile(claim, 6.0, 1000.0, Double.NEGATIVE_INFINITY);
        assertEquals(24.0, ledger.available(30.0), EPS);
        ledger.reconcile(claim, 20.0, 1000.0, Double.NEGATIVE_INFINITY);
        assertEquals(10.0, ledger.available(30.0), EPS);
        ledger.reconcile(claim, -5.0, 1000.0, Double.NEGATIVE_INFINITY);
        assertEquals(0.0, claim.amount(), EPS);
        ledger.resolve(claim, ClaimLedger.State.SETTLED);
        ledger.reconcile(claim, 99.0, 1000.0, Double.NEGATIVE_INFINITY);
        assertEquals(0.0, claim.amount(), EPS);
    }

    @Test
    void aClaimResolvesExactlyOnce() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim claim = ledger.register(false, 12.0);
        assertTrue(ledger.resolve(claim, ClaimLedger.State.SETTLED));
        assertFalse(ledger.resolve(claim, ClaimLedger.State.RELEASED));
        assertFalse(ledger.resolve(claim, ClaimLedger.State.SETTLED));
        assertEquals(ClaimLedger.State.SETTLED, claim.state());
        assertEquals(30.0, ledger.available(30.0), EPS);
        assertTrue(ledger.isEmpty());
    }

    @Test
    void resolveAllEndsEveryOpenClaim() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim a = ledger.register(false, 5.0);
        ClaimLedger.Claim b = ledger.register(true, 2.0);
        assertEquals(2, ledger.resolveAll(ClaimLedger.State.EXPIRED));
        assertEquals(ClaimLedger.State.EXPIRED, a.state());
        assertEquals(ClaimLedger.State.EXPIRED, b.state());
        assertEquals(0, ledger.resolveAll(ClaimLedger.State.EXPIRED));
        assertTrue(ledger.isEmpty());
    }

    @Test
    void openIsNeverATargetState() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim claim = ledger.register(false, 5.0);
        assertFalse(ledger.resolve(claim, ClaimLedger.State.OPEN));
        assertEquals(0, ledger.resolveAll(ClaimLedger.State.OPEN));
        assertTrue(claim.isOpen());
    }

    @Test
    void continuousReserveNeverExceedsAvailableAndNeverGoesNegative() {
        assertEquals(0.4, ClaimLedger.reserve(0.4, 30.0), EPS);
        assertEquals(0.25, ClaimLedger.reserve(0.4, 0.25), EPS);
        assertEquals(0.0, ClaimLedger.reserve(0.4, -3.0), EPS);
        assertEquals(0.0, ClaimLedger.reserve(-1.0, 5.0), EPS);
    }

    @Test
    void repricingFromZeroToPositiveHoldsTheNewCost() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim claim = ledger.register(false, 0.0);
        assertEquals(10.0, ledger.available(10.0), EPS);
        ledger.reconcile(claim, 4.0, 1000.0, Double.NEGATIVE_INFINITY);
        assertEquals(6.0, ledger.available(10.0), EPS);
    }

    @Test
    void releasedAndExpiredClaimsGiveTheirStaminaBack() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim released = ledger.register(false, 5.0);
        ClaimLedger.Claim expired = ledger.register(true, 2.0);
        ledger.resolve(released, ClaimLedger.State.RELEASED);
        assertEquals(8.0, ledger.available(10.0), EPS);
        ledger.resolve(expired, ClaimLedger.State.EXPIRED);
        assertEquals(10.0, ledger.available(10.0), EPS);
        assertEquals(0.0, ledger.reserved(), EPS);
    }

    @Test
    void availableExceptLeavesOutOnlyThatClaim() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim a = ledger.register(false, 3.0);
        ledger.register(false, 4.0);
        ledger.register(true, 1.0);
        assertEquals(15.0, ledger.availableExcept(20.0, a), EPS);
        assertEquals(12.0, ledger.available(20.0), EPS);
    }

    @Test
    void aClaimFromAnotherLedgerIsNotResolvedHere() {
        ClaimLedger mine = new ClaimLedger();
        ClaimLedger other = new ClaimLedger();
        ClaimLedger.Claim foreign = other.register(false, 5.0);
        assertFalse(mine.resolve(foreign, ClaimLedger.State.SETTLED));
        assertTrue(foreign.isOpen());
        assertEquals(5.0, other.available(10.0), EPS);
    }

    @Test
    void aDrainBesideAReserveTakesOnlyTheRest() {
        // REQ-089 criterion 2: Stamina 5, a reserve of 2; a sprint tick asking 10 may drain 3.
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim channel = ledger.register(true, ClaimLedger.reserve(2.0, ledger.available(5.0)));
        assertEquals(2.0, channel.amount(), EPS);
        ClaimLedger.Claim sprint = ledger.register(true, 10.0);
        ledger.reconcile(sprint, 10.0, 5.0, -60.0);
        assertEquals(3.0, sprint.amount(), EPS);
        assertEquals(0.0, ledger.available(5.0), EPS);
    }

    @Test
    void aReserveIsAtMostTheStaminaLeftAboveZeroSoNoDebt() {
        ClaimLedger ledger = new ClaimLedger();
        ledger.register(false, 4.0);
        ClaimLedger.Claim tick = ledger.register(true, 0.0);
        ledger.reconcile(tick, 2.0, 5.0, -60.0);
        assertEquals(1.0, tick.amount(), EPS);
        ClaimLedger.Claim late = ledger.register(true, 0.0);
        ledger.reconcile(late, 2.0, 5.0, -60.0);
        assertEquals(0.0, late.amount(), EPS);
    }

    @Test
    void spendStopsAboveTheReserves() {
        // REQ-089 criterion 4: Stamina 5, a reserve of 2; spending 10 spends 3.
        ClaimLedger ledger = new ClaimLedger();
        ledger.register(true, 2.0);
        assertEquals(3.0, ledger.spendable(10.0, 5.0, Double.NEGATIVE_INFINITY), EPS);
        assertEquals(1.0, ledger.spendable(1.0, 5.0, Double.NEGATIVE_INFINITY), EPS);
        assertEquals(0.0, ledger.spendable(4.0, 1.5, Double.NEGATIVE_INFINITY), EPS);
    }

    @Test
    void withUncappedDebtSpendIsNotLimitedByDiscreteClaims() {
        ClaimLedger ledger = new ClaimLedger();
        assertEquals(10.0, ledger.spendable(10.0, 5.0, Double.NEGATIVE_INFINITY), EPS);
        ledger.register(false, 12.0);
        assertEquals(10.0, ledger.spendable(10.0, 5.0, Double.NEGATIVE_INFINITY), EPS);
        assertEquals(0.0, ledger.spendable(-3.0, 5.0, Double.NEGATIVE_INFINITY), EPS);
    }

    @Test
    void settlingAReserveStopsOnlyWhenItTakesTheLastStamina() {
        // REQ-094 criterion 1: a reserve of the last 2 Stamina settles to 0 (stop); a reserve of 2 out of 5 does not.
        StaminaPool last = new StaminaPool(2.0);
        ClaimLedger lastLedger = new ClaimLedger();
        ClaimLedger.Claim lastTick = lastLedger.register(true, ClaimLedger.reserve(2.0, lastLedger.available(last.stamina())));
        lastLedger.resolve(lastTick, ClaimLedger.State.SETTLED);
        assertEquals(2.0, last.drainContinuous(lastTick.amount(), 0.75), EPS);
        assertFalse(last.canStart());
        assertEquals(0.0, last.stamina(), EPS);

        StaminaPool more = new StaminaPool(5.0);
        ClaimLedger moreLedger = new ClaimLedger();
        ClaimLedger.Claim moreTick = moreLedger.register(true, ClaimLedger.reserve(2.0, moreLedger.available(more.stamina())));
        moreLedger.resolve(moreTick, ClaimLedger.State.SETTLED);
        more.drainContinuous(moreTick.amount(), 0.75);
        assertTrue(more.canStart());
        assertEquals(3.0, more.stamina(), EPS);
    }

    @Test
    void reservesCountOnlyContinuousClaims() {
        ClaimLedger ledger = new ClaimLedger();
        ledger.register(false, 12.0);
        ledger.register(true, 2.0);
        ledger.register(true, 0.5);
        assertEquals(2.5, ledger.reserved(), EPS);
        assertEquals(5.5, ledger.available(20.0), EPS);
    }

    @Test
    void aSpendBesideADiscreteClaimStopsAboveItsPayableAmount() {
        // Stamina 20, debt floor -60, claim 12: spend 80 stops at -48 (68 spent); settling then pays 12 to -60.
        StaminaPool pool = new StaminaPool(20.0);
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 12.0);
        ledger.reconcile(cast, 12.0, pool.stamina(), -60.0);
        assertEquals(12.0, cast.payable(), EPS);
        pool.payDiscrete(ledger.spendable(80.0, pool.stamina(), -60.0), ledger.raisedFloor(-60.0, null), 0.75);
        assertEquals(-48.0, pool.stamina(), EPS);
        ledger.resolve(cast, ClaimLedger.State.SETTLED);
        pool.payDiscrete(cast.payable(), ledger.raisedFloor(-60.0, null), 0.75);
        assertEquals(-60.0, pool.stamina(), EPS);
    }

    @Test
    void aDirectActionBesideADiscreteClaimStopsAboveItsPayableAmount() {
        // The same claim; a direct action of 75 may start (Available 8) and pays down to -48 only.
        StaminaPool pool = new StaminaPool(20.0);
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 12.0);
        ledger.reconcile(cast, 12.0, pool.stamina(), -60.0);
        assertEquals(8.0, ledger.available(pool.stamina()), EPS);
        ClaimLedger.Claim swing = ledger.register(false, 75.0);
        ledger.reconcile(swing, 75.0, pool.stamina(), -60.0);
        ledger.resolve(swing, ClaimLedger.State.SETTLED);
        pool.payDiscrete(swing.payable(), ledger.raisedFloor(-60.0, null), 0.75);
        assertEquals(-48.0, pool.stamina(), EPS);
        ledger.resolve(cast, ClaimLedger.State.SETTLED);
        pool.payDiscrete(cast.payable(), ledger.raisedFloor(-60.0, null), 0.75);
        assertEquals(-60.0, pool.stamina(), EPS);
    }

    @Test
    void thePayableAmountIsTruncatedAtPreparation() {
        // Stamina 5, debt floor -10, final cost 20: Payable Amount 15; settling leaves -10.
        StaminaPool pool = new StaminaPool(5.0);
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 20.0);
        ledger.reconcile(cast, 20.0, pool.stamina(), -10.0);
        assertEquals(15.0, cast.payable(), EPS);
        assertEquals(20.0, cast.amount(), EPS);
        ledger.resolve(cast, ClaimLedger.State.SETTLED);
        pool.payDiscrete(cast.payable(), ledger.raisedFloor(-10.0, null), 0.75);
        assertEquals(-10.0, pool.stamina(), EPS);
    }

    @Test
    void otherClaimsAndReservesShrinkTheRoom() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim first = ledger.register(false, 30.0);
        ledger.reconcile(first, 30.0, 10.0, -40.0);
        assertEquals(30.0, first.payable(), EPS);
        ledger.register(true, 5.0);
        ClaimLedger.Claim second = ledger.register(false, 30.0);
        ledger.reconcile(second, 30.0, 10.0, -40.0);
        // 10 - (-40 + 30) - 5 = 15
        assertEquals(15.0, second.payable(), EPS);
        assertEquals(-40.0 + 30.0 + 15.0, ledger.raisedFloor(-40.0, null), EPS);
        assertEquals(-40.0 + 15.0, ledger.raisedFloor(-40.0, first), EPS);
    }

    @Test
    void uncappedDebtNeverTruncatesAndKeepsTheFloorUncapped() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 500.0);
        ledger.reconcile(cast, 500.0, 1.0, Double.NEGATIVE_INFINITY);
        assertEquals(500.0, cast.payable(), EPS);
        assertEquals(Double.NEGATIVE_INFINITY, ledger.raisedFloor(Double.NEGATIVE_INFINITY, null), 0.0);
    }

    @Test
    void withoutDiscreteClaimsTheRaisedFloorIsTheDebtFloor() {
        ClaimLedger ledger = new ClaimLedger();
        assertEquals(-60.0, ledger.raisedFloor(-60.0, null), EPS);
        ledger.register(true, 2.0);
        assertEquals(-60.0, ledger.raisedFloor(-60.0, null), EPS);
    }

    @Test
    void aResolvedClaimNoLongerRaisesTheFloor() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 12.0);
        ledger.reconcile(cast, 12.0, 20.0, -60.0);
        ledger.resolve(cast, ClaimLedger.State.RELEASED);
        assertEquals(-60.0, ledger.raisedFloor(-60.0, null), EPS);
    }

    @Test
    void spendStopsAboveADiscreteClaimsPayableAmount() {
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 12.0);
        ledger.reconcile(cast, 12.0, 20.0, -60.0);
        assertEquals(68.0, ledger.spendable(80.0, 20.0, -60.0), EPS);
        assertEquals(10.0, ledger.spendable(10.0, 20.0, -60.0), EPS);
        assertEquals(0.0, ledger.spendable(10.0, -48.0, -60.0), EPS);
    }

    @Test
    void aFloorAboveStaminaWhilePricingNeverRaisesStamina() {
        // Defensive case: a raised floor above Stamina (Stamina 5, an unlimited claim of 70, floor 10). The
        // gate's own registration limits every claim, so it does not create this state; if it ever
        // arises, a spend of 1 spends nothing and no payment raises Stamina.
        StaminaPool pool = new StaminaPool(5.0);
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim cast = ledger.register(false, 70.0);
        assertEquals(0.0, ledger.spendable(1.0, pool.stamina(), -60.0), EPS);
        pool.payDiscrete(1.0, ledger.raisedFloor(-60.0, null), 0.75);
        assertEquals(5.0, pool.stamina(), EPS);
        ledger.reconcile(cast, 70.0, pool.stamina(), -60.0);
        assertEquals(65.0, cast.payable(), EPS);
    }

    @Test
    void aTruncatedPaymentLandsExactlyOnTheFloor() {
        StaminaPool pool = new StaminaPool(36.112);
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim swing = ledger.register(false, 139.28);
        ledger.reconcile(swing, 139.28, pool.stamina(), -30.0);
        ledger.resolve(swing, ClaimLedger.State.SETTLED);
        pool.payDiscrete(swing.payable(), ledger.raisedFloor(-30.0, null), 0.75);
        assertEquals(-30.0, pool.stamina(), 0.0);
    }

    @Test
    void aClaimSettledWhileAnotherIsPricedStillPaysItsPayableAmount() {
        // Stamina 20, floor -60: A (12) is open; B (70) is registered for pricing; a listener settles A.
        // Uses the registration entry the gate uses; an unlimited provisional B (70) would raise A's
        // floor to 10, so A would pay 10 instead of 12.
        StaminaPool pool = new StaminaPool(20.0);
        ClaimLedger ledger = new ClaimLedger();
        ClaimLedger.Claim a = ledger.register(false, 12.0, pool.stamina(), -60.0);
        ClaimLedger.Claim b = ledger.register(false, 70.0, pool.stamina(), -60.0);
        assertEquals(68.0, b.payable(), EPS);
        ledger.resolve(a, ClaimLedger.State.SETTLED);
        pool.payDiscrete(a.payable(), ledger.raisedFloor(-60.0, a), 0.75);
        assertEquals(8.0, pool.stamina(), EPS);
        ledger.reconcile(b, 70.0, pool.stamina(), -60.0);
        ledger.resolve(b, ClaimLedger.State.SETTLED);
        pool.payDiscrete(b.payable(), ledger.raisedFloor(-60.0, b), 0.75);
        assertEquals(-60.0, pool.stamina(), EPS);
    }
}
