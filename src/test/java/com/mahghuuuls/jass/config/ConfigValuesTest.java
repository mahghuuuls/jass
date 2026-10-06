package com.mahghuuuls.jass.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValuesTest {

    @Test
    void zeroPositiveAndMinusOneAreValidDebtFractions() {
        assertTrue(ConfigValues.isValidDebtFraction(0.0));
        assertTrue(ConfigValues.isValidDebtFraction(1.5));
        assertTrue(ConfigValues.isValidDebtFraction(-1.0));
        assertEquals(-1.0, ConfigValues.debtFraction(-1.0), 0.0);
        assertEquals(0.5, ConfigValues.debtFraction(0.5), 0.0);
    }

    @Test
    void negativeValuesOtherThanMinusOneFallBackToTheDefault() {
        assertFalse(ConfigValues.isValidDebtFraction(-0.5));
        assertEquals(1.0, ConfigValues.debtFraction(-0.5), 0.0);
    }
}
