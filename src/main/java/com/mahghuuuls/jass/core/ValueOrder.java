package com.mahghuuuls.jass.core;

/**
 * The order JASS reads a per-item value in (REQ-086): the owner's configuration entry, else an
 * addon provider's value, else the default. {@code NaN} means "no value" at each step.
 */
public final class ValueOrder {

    private ValueOrder() {
    }

    public static double first(double configured, double provided, double fallback) {
        return !Double.isNaN(configured) ? configured : !Double.isNaN(provided) ? provided : fallback;
    }
}
