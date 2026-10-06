package com.mahghuuuls.jass.core;

/** A player's resolved Stamina stats at one moment. */
public final class StaminaProfile {

    private final double maximum;
    private final double regeneration;
    private final double regenerationDelay;
    private final double efficiency;

    public StaminaProfile(double maximum, double regeneration, double regenerationDelay, double efficiency) {
        this.maximum = maximum;
        this.regeneration = regeneration;
        this.regenerationDelay = regenerationDelay;
        this.efficiency = efficiency;
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
}
