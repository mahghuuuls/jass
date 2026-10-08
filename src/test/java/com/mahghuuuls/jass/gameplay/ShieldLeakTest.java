package com.mahghuuuls.jass.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShieldLeakTest {

    private static final double EPS = 1e-6;

    @Test
    void meleeFractionPointSevenLetsThirtyPercentThrough() {
        assertEquals(0.9, ShieldRules.leakedDamage(3.0, 0.7), EPS);
        assertEquals(3.0, ShieldRules.leakedDamage(10.0, 0.7), EPS);
    }

    @Test
    void projectileFractionHalfLetsHalfThrough() {
        assertEquals(2.0, ShieldRules.leakedDamage(4.0, 0.5), EPS);
    }

    @Test
    void fullBlockLetsNothingThrough() {
        assertEquals(0.0, ShieldRules.leakedDamage(9.0, 1.0), EPS);
    }

    @Test
    void fractionsOutsideZeroToOneAreClamped() {
        assertEquals(0.0, ShieldRules.leakedDamage(9.0, 1.5), EPS);
        assertEquals(9.0, ShieldRules.leakedDamage(9.0, -0.5), EPS);
    }
}
