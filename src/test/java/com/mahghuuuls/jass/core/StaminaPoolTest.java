package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaPoolTest {

    private static final double EPS = 1e-9;
    private static final double TICK = 0.05;

    @Test
    void discreteActionMayOverdrawIntoDebt() {
        StaminaPool pool = new StaminaPool(3.0);
        assertTrue(pool.canStart());
        pool.payDiscrete(12.0, StaminaPool.debtFloor(60.0, 1.0), 0.75);
        assertEquals(-9.0, pool.stamina(), EPS);
        assertEquals(0.0, pool.visible(), EPS);
        assertEquals(9.0, pool.debt(), EPS);
    }

    @Test
    void nothingStartsAtZeroOrBelow() {
        assertFalse(new StaminaPool(0.0).canStart());
        assertFalse(new StaminaPool(-0.5).canStart());
    }

    @Test
    void debtIsCappedByTheConfiguredFraction() {
        StaminaPool pool = new StaminaPool(5.0);
        pool.payDiscrete(50.0, StaminaPool.debtFloor(60.0, 0.5), 0.75);
        assertEquals(-30.0, pool.stamina(), EPS);
    }

    @Test
    void negativeFractionMeansUncappedDebt() {
        StaminaPool pool = new StaminaPool(5.0);
        pool.payDiscrete(500.0, StaminaPool.debtFloor(60.0, -1.0), 0.75);
        assertEquals(-495.0, pool.stamina(), EPS);
    }

    @Test
    void continuousDrainStopsExactlyAtZero() {
        StaminaPool pool = new StaminaPool(2.0);
        assertEquals(2.0, pool.drainContinuous(5.0, 0.75), EPS);
        assertEquals(0.0, pool.stamina(), EPS);
        assertEquals(0.0, pool.drainContinuous(5.0, 0.75), EPS);
        assertEquals(0.0, pool.stamina(), EPS);
    }

    @Test
    void regenerationWaitsForTheDelayThenRepaysDebtFirst() {
        StaminaPool pool = new StaminaPool(0.0);
        pool.set(-10.0, StaminaPool.debtFloor(60.0, 1.0), 60.0, 0.75);
        // 0.75 s of delay: nothing changes.
        for (int i = 0; i < 15; i++) {
            pool.tick(TICK, 25.0, 60.0);
        }
        assertEquals(-10.0, pool.stamina(), EPS);
        // 10 / 25 = 0.4 s more repays the debt; the visible value is still zero at that point.
        for (int i = 0; i < 8; i++) {
            pool.tick(TICK, 25.0, 60.0);
        }
        assertEquals(0.0, pool.stamina(), 1e-6);
        // 60 / 25 = 2.4 s more fills the bar.
        for (int i = 0; i < 48; i++) {
            pool.tick(TICK, 25.0, 60.0);
        }
        assertEquals(60.0, pool.stamina(), 1e-6);
    }

    @Test
    void aRaisedShieldAtZeroShareLetsTheDelayRunButRepaysNothing() {
        StaminaPool pool = new StaminaPool(0.0);
        pool.set(-10.0, StaminaPool.debtFloor(60.0, 1.0), 60.0, 0.75);
        double raised = RegenerationRules.rate(25.0, true, 0.0);
        for (int i = 0; i < 20; i++) {
            pool.tick(TICK, raised, 60.0);
        }
        assertEquals(0.0, pool.delayRemaining(), EPS);
        assertEquals(-10.0, pool.stamina(), EPS);
        // Lowered: the full rate at once, no new delay.
        pool.tick(TICK, RegenerationRules.rate(25.0, false, 0.0), 60.0);
        assertEquals(-10.0 + 25.0 * TICK, pool.stamina(), EPS);
    }

    @Test
    void delayRemainderCarriesIntoRegenerationWithinOneTick() {
        StaminaPool pool = new StaminaPool(10.0);
        pool.payDiscrete(5.0, -60.0, 0.02);
        pool.tick(TICK, 100.0, 60.0);
        assertEquals(5.0 + 100.0 * 0.03, pool.stamina(), EPS);
    }

    @Test
    void aDrainingTickRegeneratesNothingEvenWithZeroDelay() {
        StaminaPool pool = new StaminaPool(30.0);
        pool.drainContinuous(0.25, 0.0);
        pool.tick(TICK, 25.0, 60.0);
        assertEquals(29.75, pool.stamina(), EPS);
        pool.tick(TICK, 25.0, 60.0);
        assertEquals(29.75 + 1.25, pool.stamina(), EPS);
    }

    @Test
    void positiveStaminaIsClampedWhenTheMaximumDrops() {
        StaminaPool pool = new StaminaPool(80.0);
        pool.tick(TICK, 25.0, 60.0);
        assertEquals(60.0, pool.stamina(), EPS);
    }

    @Test
    void debtIsKeptWhenTheMaximumDrops() {
        StaminaPool pool = new StaminaPool(0.0);
        pool.set(-10.0, -80.0, 80.0, 0.75);
        pool.tick(TICK, 25.0, 60.0);
        assertEquals(-10.0, pool.stamina(), EPS);
    }

    @Test
    void setIsClampedToFloorAndMaximum() {
        StaminaPool pool = new StaminaPool(60.0);
        pool.set(-500.0, StaminaPool.debtFloor(60.0, 1.0), 60.0, 0.75);
        assertEquals(-60.0, pool.stamina(), EPS);
        pool.set(500.0, StaminaPool.debtFloor(60.0, 1.0), 60.0, 0.75);
        assertEquals(60.0, pool.stamina(), EPS);
    }

    @Test
    void restoreFillsAndClearsTheDelay() {
        StaminaPool pool = new StaminaPool(0.0);
        pool.set(-20.0, -60.0, 60.0, 0.75);
        pool.restore(60.0);
        assertEquals(60.0, pool.stamina(), EPS);
        assertEquals(0.0, pool.delayRemaining(), EPS);
    }

    @Test
    void aReserveIsPaidInFullEvenAfterAnotherActionWentIntoDebt() {
        // Stamina 5, a reserve of 2 (Available 3); a jump of 10 meanwhile pays in full; the reserve is still paid.
        StaminaPool pool = new StaminaPool(5.0);
        pool.payDiscrete(10.0, StaminaPool.debtFloor(60.0, -1.0), 0.75);
        assertEquals(2.0, pool.payReserve(2.0, StaminaPool.debtFloor(60.0, -1.0), 0.75), EPS);
        assertEquals(-7.0, pool.stamina(), EPS);
        assertFalse(pool.canStart());
    }

    @Test
    void aReserveWithoutOtherActionsEndsAtOrAboveZeroAndStopsAtTheDebtFloor() {
        StaminaPool pool = new StaminaPool(2.0);
        assertEquals(2.0, pool.payReserve(2.0, -30.0, 0.75), EPS);
        assertEquals(0.0, pool.stamina(), EPS);
        StaminaPool floored = new StaminaPool(-29.0);
        assertEquals(1.0, floored.payReserve(2.0, -30.0, 0.75), EPS);
        assertEquals(-30.0, floored.stamina(), EPS);
        assertEquals(0.0, floored.payReserve(0.0, -30.0, 0.75), EPS);
    }

    @Test
    void payingAReservePausesRegenerationForTheTick() {
        StaminaPool pool = new StaminaPool(10.0);
        pool.payReserve(2.0, -30.0, 0.0);
        pool.tick(0.05, 20.0, 60.0);
        assertEquals(8.0, pool.stamina(), EPS);
    }

    @Test
    void theDelayEndsWithoutATinyRegenerationFromRounding() {
        // 0.75 counted down in 0.05 steps leaves a residue; it must not become a sliver of positive
        // Stamina, or a held sprint starts again at once and restarts the delay forever.
        StaminaPool pool = new StaminaPool(0.0);
        pool.set(0.0, -60.0, 60.0, 0.75);
        for (int i = 0; i < 15; i++) {
            pool.tick(TICK, 25.0, 60.0);
        }
        assertEquals(0.0, pool.stamina(), 0.0);
        assertFalse(pool.canStart());
        assertEquals(0.0, pool.delayRemaining(), 0.0);
        pool.tick(TICK, 25.0, 60.0);
        assertEquals(1.25, pool.stamina(), EPS);
    }

    @Test
    void aLongDelayAlsoEndsWithoutATinyRegeneration() {
        // The countdown residue grows with the delay (about 4e-9 s at 3600 s).
        StaminaPool pool = new StaminaPool(0.0);
        pool.set(0.0, -60.0, 60.0, 3600.0);
        for (int i = 0; i < 72000; i++) {
            pool.tick(TICK, 25.0, 60.0);
        }
        assertFalse(pool.canStart());
        pool.tick(TICK, 25.0, 60.0);
        assertEquals(1.25, pool.stamina(), EPS);
    }

    @Test
    void freeDiscreteCostChangesNothing() {
        StaminaPool pool = new StaminaPool(30.0);
        pool.payDiscrete(0.0, -60.0, 0.75);
        assertEquals(30.0, pool.stamina(), EPS);
        assertEquals(0.0, pool.delayRemaining(), EPS);
    }
}
