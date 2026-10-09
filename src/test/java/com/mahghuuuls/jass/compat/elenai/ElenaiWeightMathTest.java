package com.mahghuuuls.jass.compat.elenai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElenaiWeightMathTest {

    private static final double EPS = 1e-9;

    @Test
    void fullDiamondWithElenaiDefaultsIsSixteenUnitsAndTwentyTwoPointFour() {
        int weight = ElenaiWeightMath.elenaiWeight(3 + 6 + 4 + 3, 0, false);
        assertEquals(16, weight);
        assertEquals(22.4, ElenaiWeightMath.effectiveWeight(weight, 1.4), EPS);
    }

    @Test
    void armorPointsUseIntegerDivisionThenTimesOnePointEight() {
        assertEquals(5.4, ElenaiWeightMath.pieceFromArmorPoints(7), EPS);
        assertEquals(1.8, ElenaiWeightMath.pieceFromArmorPoints(3), EPS);
    }

    @Test
    void oddWeightsRoundDownToEvenUnlessHalfFeathers() {
        assertEquals(10, ElenaiWeightMath.elenaiWeight(11.9, 0, false));
        assertEquals(11, ElenaiWeightMath.elenaiWeight(11.9, 0, true));
    }

    @Test
    void lightweightLevelsReduceTheWeight() {
        assertEquals(12, ElenaiWeightMath.elenaiWeight(16, 3, false));
    }

    @Test
    void weightNeverGoesNegative() {
        assertEquals(0.0, ElenaiWeightMath.effectiveWeight(-6, 1.4), EPS);
        assertEquals(0.0, ElenaiWeightMath.effectiveWeight(16, -1.0), EPS);
    }
}
