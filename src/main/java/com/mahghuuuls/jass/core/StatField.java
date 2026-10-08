package com.mahghuuuls.jass.core;

import java.util.Locale;

/** The modifier fields an item rule or provider can set (REQ-040), with their configuration names. */
public enum StatField {
    FLAT_MAXIMUM,
    MAXIMUM_INCREASE,
    MAXIMUM_REDUCTION,
    FLAT_REGENERATION,
    REGENERATION_INCREASE,
    REGENERATION_REDUCTION,
    FLAT_DELAY,
    DELAY_INCREASE,
    DELAY_REDUCTION,
    EFFICIENCY;

    /** The name used in configuration lines, such as {@code flat_maximum}. */
    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** True for the reduction fields, whose values must be fractions from 0 to 1. */
    public boolean isReduction() {
        return this == MAXIMUM_REDUCTION || this == REGENERATION_REDUCTION || this == DELAY_REDUCTION;
    }

    /** The field with this configuration name, or {@code null}. */
    public static StatField byConfigName(String name) {
        for (StatField field : values()) {
            if (field.configName().equals(name)) {
                return field;
            }
        }
        return null;
    }
}
