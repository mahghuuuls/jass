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
    private final double weightBeforeModifiers;
    private final double stabilityBonus;

    public StaminaProfile(double maximum, double regeneration, double regenerationDelay, double efficiency,
            double effectiveWeight, String weightSource, double heldWeight, List<String> contributions,
            double weightBeforeModifiers, double stabilityBonus) {
        this.weightBeforeModifiers = weightBeforeModifiers;
        this.stabilityBonus = stabilityBonus;
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

    /** Weight of worn armor plus held items, after weight modifiers; multiplies Movement Action costs. */
    public double effectiveWeight() {
        return effectiveWeight;
    }

    /** Armor weight from the active source plus held weight, before weight modifiers (REQ-095). */
    public double weightBeforeModifiers() {
        return weightBeforeModifiers;
    }

    /** Points added to the blocking item's Stability in this player's Shield Blocks; may be negative (REQ-095). */
    public double stabilityBonus() {
        return stabilityBonus;
    }

    /** The part of {@link #weightBeforeModifiers()} that comes from the items in both hands. */
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
