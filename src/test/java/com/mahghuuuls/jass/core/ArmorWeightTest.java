package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorWeightTest {

    private static final double EPS = 1e-9;

    @Test
    void configuredWeightWinsOverArmorPoints() {
        assertEquals(6.0, ArmorWeight.itemWeight(6.0, 6.0 * 3, 1.0), EPS);
    }

    @Test
    void configuredZeroMakesArmorWeightless() {
        assertEquals(0.0, ArmorWeight.itemWeight(0.0, 8.0, 1.0), EPS);
    }

    @Test
    void unconfiguredBootsWithTwoArmorPointsWeighTwo() {
        assertEquals(2.0, ArmorWeight.itemWeight(Double.NaN, 2.0, 1.0), EPS);
    }

    @Test
    void fallbackScalesWithTheConfiguredValue() {
        assertEquals(3.0, ArmorWeight.itemWeight(Double.NaN, 2.0, 1.5), EPS);
    }

    @Test
    void itemWithoutArmorPointsWeighsNothing() {
        assertEquals(0.0, ArmorWeight.itemWeight(Double.NaN, 0.0, 1.0), EPS);
    }

    @Test
    void fullIronSetAtDefaultsMakesSprintCostThirteenPointFourOverTwoSeconds() {
        double weight = ArmorWeight.itemWeight(3.0, 2.0, 1.0) + ArmorWeight.itemWeight(6.0, 6.0, 1.0)
                + ArmorWeight.itemWeight(5.0, 5.0, 1.0) + ArmorWeight.itemWeight(3.0, 2.0, 1.0);
        assertEquals(17.0, weight, EPS);
        double factors = CostCalculator.weightMultiplier(weight, 0.02);
        assertEquals(13.4, 40 * CostCalculator.continuousCost(5.0, factors, 1.0 / 20.0), EPS);
    }
}
