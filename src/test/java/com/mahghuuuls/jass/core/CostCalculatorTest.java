package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostCalculatorTest {

    private static final double EPS = 1e-9;

    @Test
    void efficiencyOfOneScaleHalvesCosts() {
        double factor = CostCalculator.efficiencyFactor(100.0, 100.0);
        assertEquals(6.0, CostCalculator.finalDiscreteCost(12.0, factor, 1.0), EPS);
    }

    @Test
    void zeroEfficiencyLeavesCostsUnchanged() {
        assertEquals(1.0, CostCalculator.efficiencyFactor(100.0, 0.0), EPS);
    }

    @Test
    void negativeEfficiencyCountsAsZero() {
        assertEquals(1.0, CostCalculator.efficiencyFactor(100.0, -50.0), EPS);
    }

    @Test
    void minimumCostAppliesToPositiveBaseCosts() {
        assertEquals(1.0, CostCalculator.finalDiscreteCost(2.0, 0.1, 1.0), EPS);
    }

    @Test
    void zeroBaseCostStaysFree() {
        assertEquals(0.0, CostCalculator.finalDiscreteCost(0.0, 1.0, 1.0), EPS);
    }

    @Test
    void weightOfFiftyDoublesMovementCosts() {
        assertEquals(2.0, CostCalculator.weightMultiplier(50.0, 0.02), EPS);
    }

    @Test
    void vanillaShieldStabilityMakesANineDamageBlockCostSix() {
        double factor = CostCalculator.stabilityFactor(100.0, 50.0);
        assertEquals(6.0, 9.0 * 1.0 * factor, EPS);
    }

    @Test
    void negativeStabilityCountsAsZero() {
        assertEquals(1.0, CostCalculator.stabilityFactor(100.0, -10.0), EPS);
    }
}
