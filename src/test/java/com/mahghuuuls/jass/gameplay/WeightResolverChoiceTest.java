package com.mahghuuuls.jass.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightResolverChoiceTest {

    @Test
    void usablePreferredWeightWins() {
        WeightResolver.Resolved r = WeightResolver.choose(22.4, "elenai", () -> 23.0);
        assertEquals(22.4, r.weight, 1e-9);
        assertEquals("elenai", r.source);
    }

    @Test
    void notANumberFallsBackToStandalone() {
        WeightResolver.Resolved r = WeightResolver.choose(Double.NaN, "elenai", () -> 23.0);
        assertEquals(23.0, r.weight, 1e-9);
        assertEquals(WeightResolver.STANDALONE, r.source);
    }

    @Test
    void negativeWeightFallsBackToStandalone() {
        WeightResolver.Resolved r = WeightResolver.choose(-1.0, "elenai", () -> 17.0);
        assertEquals(17.0, r.weight, 1e-9);
        assertEquals(WeightResolver.STANDALONE, r.source);
    }
}
