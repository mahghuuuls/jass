package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegenerationRulesTest {

    @Test
    void raisedBlockingItemKeepsItsShare() {
        assertEquals(5.0, RegenerationRules.rate(25.0, true, 0.2), 1e-9);
        assertEquals(0.0, RegenerationRules.rate(25.0, true, 0.0), 1e-9);
        assertEquals(25.0, RegenerationRules.rate(25.0, true, 1.0), 1e-9);
    }

    @Test
    void loweredItemOrBadShareKeepsTheFullRate() {
        assertEquals(25.0, RegenerationRules.rate(25.0, false, 0.2), 1e-9);
        assertEquals(25.0, RegenerationRules.rate(25.0, true, Double.NaN), 1e-9);
    }

    @Test
    void shareIsLimitedToZeroToOne() {
        assertEquals(25.0, RegenerationRules.rate(25.0, true, 3.0), 1e-9);
        assertEquals(0.0, RegenerationRules.rate(25.0, true, -1.0), 1e-9);
    }
}
