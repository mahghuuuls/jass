package com.mahghuuuls.jass.core;

import java.util.Collections;
import java.util.List;

/** A player's resolved Stamina stats at one moment, with the contributions that produced them. */
public final class StaminaProfile {

    private final double maximum;
    private final double regeneration;
    private final double regenerationDelay;
    private final double efficiency;
    private final double effectiveWeight;
    private final String weightSource;
    private final double heldWeight;
    private final List<String> contributions;

    public StaminaProfile(double maximum, double regeneration, double regenerationDelay, double efficiency,
            double effectiveWeight, String weightSource) {
        this(maximum, regeneration, regenerationDelay, efficiency, effectiveWeight, weightSource,
                Collections.<String>emptyList());
    }

    public StaminaProfile(double maximum, double regeneration, double regenerationDelay, double efficiency,
            double effectiveWeight, String weightSource, List<String> contributions) {
        this(maximum, regeneration, regenerationDelay, efficiency, effectiveWeight, weightSource, 0.0, contributions);
    }

    public StaminaProfile(double maximum, double regeneration, double regenerationDelay, double efficiency,
            double effectiveWeight, String weightSource, double heldWeight, List<String> contributions) {
        this.heldWeight = heldWeight;
        this.contributions = Collections.unmodifiableList(contributions);
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

    /** Weight of worn armor plus held items; multiplies Movement Action costs. */
    public double effectiveWeight() {
        return effectiveWeight;
    }

    /** The part of {@link #effectiveWeight()} that comes from the items in both hands. */
    public double heldWeight() {
        return heldWeight;
    }

    /** One line per contributing item rule or provider, for {@code /stamina inspect}. */
    public List<String> contributions() {
        return contributions;
    }

    /** Where the armor weight came from, such as {@code standalone}. */
    public String weightSource() {
        return weightSource;
    }
}
