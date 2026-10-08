package com.mahghuuuls.jass.api;

/**
 * An immutable contribution to a player's Stamina Profile (REQ-041), from a provider. Increases and
 * reductions are fractions (0.5 = 50 percent); reductions are clamped to 0..1 when combined.
 */
public final class StaminaContribution {

    public static final StaminaContribution EMPTY = builder().build();

    private final double flatMaximum;
    private final double maximumIncrease;
    private final double maximumReduction;
    private final double flatRegeneration;
    private final double regenerationIncrease;
    private final double regenerationReduction;
    private final double flatDelay;
    private final double delayIncrease;
    private final double delayReduction;
    private final double efficiency;

    private StaminaContribution(Builder b) {
        this.flatMaximum = b.flatMaximum;
        this.maximumIncrease = b.maximumIncrease;
        this.maximumReduction = b.maximumReduction;
        this.flatRegeneration = b.flatRegeneration;
        this.regenerationIncrease = b.regenerationIncrease;
        this.regenerationReduction = b.regenerationReduction;
        this.flatDelay = b.flatDelay;
        this.delayIncrease = b.delayIncrease;
        this.delayReduction = b.delayReduction;
        this.efficiency = b.efficiency;
    }

    public static Builder builder() {
        return new Builder();
    }

    public double getFlatMaximum() { return flatMaximum; }
    public double getMaximumIncrease() { return maximumIncrease; }
    public double getMaximumReduction() { return maximumReduction; }
    public double getFlatRegeneration() { return flatRegeneration; }
    public double getRegenerationIncrease() { return regenerationIncrease; }
    public double getRegenerationReduction() { return regenerationReduction; }
    public double getFlatDelay() { return flatDelay; }
    public double getDelayIncrease() { return delayIncrease; }
    public double getDelayReduction() { return delayReduction; }
    public double getEfficiency() { return efficiency; }

    /** True when every field is 0. */
    public boolean isEmpty() {
        return flatMaximum == 0 && maximumIncrease == 0 && maximumReduction == 0 && flatRegeneration == 0
                && regenerationIncrease == 0 && regenerationReduction == 0 && flatDelay == 0 && delayIncrease == 0
                && delayReduction == 0 && efficiency == 0;
    }

    /** Builds a contribution; values must be finite numbers. */
    public static final class Builder {
        private double flatMaximum;
        private double maximumIncrease;
        private double maximumReduction;
        private double flatRegeneration;
        private double regenerationIncrease;
        private double regenerationReduction;
        private double flatDelay;
        private double delayIncrease;
        private double delayReduction;
        private double efficiency;

        private Builder() {
        }

        public Builder flatMaximum(double value) { flatMaximum = finite(value); return this; }
        public Builder maximumIncrease(double value) { maximumIncrease = finite(value); return this; }
        public Builder maximumReduction(double value) { maximumReduction = finite(value); return this; }
        public Builder flatRegeneration(double value) { flatRegeneration = finite(value); return this; }
        public Builder regenerationIncrease(double value) { regenerationIncrease = finite(value); return this; }
        public Builder regenerationReduction(double value) { regenerationReduction = finite(value); return this; }
        public Builder flatDelay(double value) { flatDelay = finite(value); return this; }
        public Builder delayIncrease(double value) { delayIncrease = finite(value); return this; }
        public Builder delayReduction(double value) { delayReduction = finite(value); return this; }
        public Builder efficiency(double value) { efficiency = finite(value); return this; }

        public StaminaContribution build() {
            return new StaminaContribution(this);
        }

        private static double finite(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("Stamina contribution values must be finite numbers");
            }
            return value;
        }
    }
}
