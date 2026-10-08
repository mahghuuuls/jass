package com.mahghuuuls.jass.core;

/**
 * The only place Stamina cost factors are computed: Stamina Efficiency, the weight multiplier
 * for Movement Actions, shield Stability, and the minimum discrete cost.
 */
public final class CostCalculator {

    private CostCalculator() {
    }

    /** {@code scale / (scale + efficiency)}; negative total efficiency counts as zero. */
    public static double efficiencyFactor(double efficiencyScale, double staminaEfficiency) {
        double efficiency = Math.max(0.0, staminaEfficiency);
        return efficiencyScale / (efficiencyScale + efficiency);
    }

    /** {@code 1 + effectiveWeight x weightFactor}, never below 1. */
    public static double weightMultiplier(double effectiveWeight, double weightFactor) {
        return 1.0 + Math.max(0.0, effectiveWeight) * Math.max(0.0, weightFactor);
    }

    /** {@code scale / (scale + stability)}; negative Stability counts as zero. */
    public static double stabilityFactor(double stabilityScale, double stability) {
        return stabilityScale / (stabilityScale + Math.max(0.0, stability));
    }

    /**
     * Base cost of a Shield Block before efficiency and the minimum cost: the incoming damage
     * (difficulty-scaled, before armor) times Stamina per damage point times the Stability factor.
     */
    public static double blockBaseCost(double incomingDamage, double staminaPerDamage, double stabilityFactor) {
        return Math.max(0.0, incomingDamage) * Math.max(0.0, staminaPerDamage) * stabilityFactor;
    }

    /** Amount a Continuous Action drains in {@code seconds}: cost per second times all factors. */
    public static double continuousCost(double costPerSecond, double factors, double seconds) {
        return Math.max(0.0, costPerSecond) * factors * seconds;
    }

    /**
     * Final cost of a Discrete Action. A configured base cost of zero (or less) stays free;
     * any other cost is at least {@code minimumCost} after all factors.
     */
    public static double finalDiscreteCost(double baseCost, double factors, double minimumCost) {
        if (baseCost <= 0.0) {
            return 0.0;
        }
        return Math.max(minimumCost, baseCost * factors);
    }
}
