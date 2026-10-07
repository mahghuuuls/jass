package com.mahghuuuls.jass.core;

/** A player's resolved Stamina stats at one moment. */
public final class StaminaProfile {

    private final double maximum;
    private final double regeneration;
    private final double regenerationDelay;
    private final double efficiency;
    private final double effectiveWeight;
    private final String weightSource;

    public StaminaProfile(double maximum, double regeneration, double regenerationDelay, double efficiency,
            double effectiveWeight, String weightSource) {
        this.maximum = maximum;
        this.regeneration = regeneration;
        this.regenerationDelay = regenerationDelay;
        this.efficiency = efficiency;
        this.effectiveWeight = effectiveWeight;
        this.weightSource = weightSource;
    }

    public double maximum() {
        return maximum;
    }

    public double regeneration() {
        return regeneration;
    }

    public double regenerationDelay() {
        return regenerationDelay;
    }

    public double efficiency() {
        return efficiency;
    }

    /** Weight of worn armor; multiplies Movement Action costs. */
    public double effectiveWeight() {
        return effectiveWeight;
    }

    /** Where the weight came from, such as {@code standalone}. */
    public String weightSource() {
        return weightSource;
    }
}
