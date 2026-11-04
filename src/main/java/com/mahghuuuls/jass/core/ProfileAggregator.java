package com.mahghuuuls.jass.core;

import java.util.List;

/**
 * The only place contributions are combined into stats (REQ-041, REV-006). For Maximum Stamina,
 * Stamina Regeneration, and Regeneration Delay: {@code max(0, base + sum of flat) x max(0, 1 + sum of
 * increase) x product of (1 - each reduction)}, so two penalties never turn into a bonus. Stamina
 * Efficiency is the sum of every {@code efficiency}. The Stability Bonus is the sum of every
 * {@code stability}; Effective Weight applies the same formula to the source weight with
 * {@code flat_weight}, {@code weight_increase}, and {@code weight_reduction}.
 */
public final class ProfileAggregator {

    private ProfileAggregator() {
    }

    /** Resolved stats. */
    public static final class Stats {
        public final double maximum;
        public final double regeneration;
        public final double regenerationDelay;
        public final double efficiency;
        /** Sum of every {@code stability}; may be negative. */
        public final double stabilityBonus;
        /** The source weight after the weight modifiers, never negative. */
        public final double effectiveWeight;

        Stats(double maximum, double regeneration, double regenerationDelay, double efficiency,
                double stabilityBonus, double effectiveWeight) {
            this.maximum = maximum;
            this.regeneration = regeneration;
            this.regenerationDelay = regenerationDelay;
            this.efficiency = efficiency;
            this.stabilityBonus = stabilityBonus;
            this.effectiveWeight = effectiveWeight;
        }
    }

    /**
     * @param sourceWeight armor weight from the active weight source plus held weight,
     *                     before the weight modifiers
     */
    public static Stats aggregate(double baseMaximum, double baseRegeneration, double baseDelay, double sourceWeight,
            List<ModifierSet> contributions) {
        double efficiency = 0.0;
        double stabilityBonus = 0.0;
        for (ModifierSet set : contributions) {
            efficiency += set.get(StatField.EFFICIENCY);
            stabilityBonus += set.get(StatField.STABILITY);
        }
        return new Stats(
                resolve(baseMaximum, contributions, StatField.FLAT_MAXIMUM, StatField.MAXIMUM_INCREASE,
                        StatField.MAXIMUM_REDUCTION),
                resolve(baseRegeneration, contributions, StatField.FLAT_REGENERATION, StatField.REGENERATION_INCREASE,
                        StatField.REGENERATION_REDUCTION),
                resolve(baseDelay, contributions, StatField.FLAT_DELAY, StatField.DELAY_INCREASE,
                        StatField.DELAY_REDUCTION),
                efficiency, stabilityBonus,
                resolve(sourceWeight, contributions, StatField.FLAT_WEIGHT, StatField.WEIGHT_INCREASE,
                        StatField.WEIGHT_REDUCTION));
    }

    static double resolve(double base, List<ModifierSet> contributions, StatField flat, StatField increase,
            StatField reduction) {
        double flatSum = 0.0;
        double increaseSum = 0.0;
        double reductionProduct = 1.0;
        for (ModifierSet set : contributions) {
            flatSum += set.get(flat);
            increaseSum += set.get(increase);
            reductionProduct *= 1.0 - Math.min(1.0, Math.max(0.0, set.get(reduction)));
        }
        return Math.max(0.0, Math.max(0.0, base + flatSum) * Math.max(0.0, 1.0 + increaseSum) * reductionProduct);
    }
}
