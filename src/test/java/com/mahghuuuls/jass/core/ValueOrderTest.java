package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueOrderTest {

    @Test
    void configurationWinsThenProviderThenDefault() {
        assertEquals(3.0, ValueOrder.first(3.0, 5.0, 12.0));
        assertEquals(5.0, ValueOrder.first(Double.NaN, 5.0, 12.0));
        assertEquals(12.0, ValueOrder.first(Double.NaN, Double.NaN, 12.0));
        assertEquals(0.0, ValueOrder.first(0.0, 5.0, 12.0));
        assertTrue(Double.isNaN(ValueOrder.first(Double.NaN, Double.NaN, Double.NaN)));
    }
}
