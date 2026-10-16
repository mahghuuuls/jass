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
        ledger.reconcile(claim, 6.0);
        assertEquals(24.0, ledger.available(30.0), EPS);
        ledger.reconcile(claim, 20.0);
        assertEquals(10.0, ledger.available(30.0), EPS);
        ledger.reconcile(claim, -5.0);
        assertEquals(0.0, claim.amount(), EPS);
        ledger.resolve(claim, ClaimLedger.State.SETTLED);
        ledger.reconcile(claim, 99.0);
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
        ledger.reconcile(claim, 4.0);
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
        ledger.reconcile(sprint, ClaimLedger.reserve(10.0, ledger.availableExcept(5.0, sprint)));
        assertEquals(3.0, sprint.amount(), EPS);
        assertEquals(0.0, ledger.available(5.0), EPS);
    }

    @Test
    void aReserveIsAtMostTheStaminaLeftAboveZeroSoNoDebt() {
        ClaimLedger ledger = new ClaimLedger();
        ledger.register(false, 4.0);
        ClaimLedger.Claim tick = ledger.register(true, 0.0);
        ledger.reconcile(tick, ClaimLedger.reserve(2.0, ledger.availableExcept(5.0, tick)));
        assertEquals(1.0, tick.amount(), EPS);
        ClaimLedger.Claim late = ledger.register(true, 0.0);
        ledger.reconcile(late, ClaimLedger.reserve(2.0, ledger.availableExcept(5.0, late)));
        assertEquals(0.0, late.amount(), EPS);
    }

    @Test
    void spendStopsAboveTheReserves() {
        // REQ-089 criterion 4: Stamina 5, a reserve of 2; spending 10 spends 3.
        ClaimLedger ledger = new ClaimLedger();
        ledger.register(true, 2.0);
        assertEquals(3.0, ledger.spendable(10.0, 5.0), EPS);
        assertEquals(1.0, ledger.spendable(1.0, 5.0), EPS);
        assertEquals(0.0, ledger.spendable(4.0, 1.5), EPS);
    }

    @Test
    void spendIsNotLimitedByDiscreteClaimsOrWithoutClaims() {
        ClaimLedger ledger = new ClaimLedger();
        assertEquals(10.0, ledger.spendable(10.0, 5.0), EPS);
        ledger.register(false, 12.0);
        assertEquals(10.0, ledger.spendable(10.0, 5.0), EPS);
        assertEquals(0.0, ledger.spendable(-3.0, 5.0), EPS);
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
}
