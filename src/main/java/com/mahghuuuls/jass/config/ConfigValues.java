package com.mahghuuuls.jass.config;

/** Validation of individual configuration values whose meaning a plain range cannot express. */
final class ConfigValues {

    static final double DEFAULT_MAX_DEBT_FRACTION = 1.0;
    static final double UNCAPPED_DEBT = -1.0;

    private ConfigValues() {
    }

    /** {@code max_debt_fraction} is either zero or more, or exactly -1 (no limit). */
    static boolean isValidDebtFraction(double value) {
        return value >= 0.0 || value == UNCAPPED_DEBT;
    }

    /** The value to use: the configured one when valid, otherwise the default. */
    static double debtFraction(double value) {
        return isValidDebtFraction(value) ? value : DEFAULT_MAX_DEBT_FRACTION;
    }
}
