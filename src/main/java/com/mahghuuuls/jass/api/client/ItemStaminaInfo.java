package com.mahghuuuls.jass.api.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Stamina values of one item as the server configures them, before Stamina Efficiency and
 * weight scaling (REQ-058). Immutable. A value that does not apply is {@code NaN}.
 */
public final class ItemStaminaInfo {

    /** No values: before the server's values arrive, or for an item JASS has nothing to say about. */
    public static final ItemStaminaInfo NONE = new Builder().build();

    /** One configured item Stamina modifier field (REQ-040), such as {@code flat_maximum} 15. */
    public static final class Modifier {
        private final String field;
        private final double value;
        private final boolean beneficial;

        public Modifier(String field, double value, boolean beneficial) {
            this.field = field;
            this.value = value;
            this.beneficial = beneficial;
        }

        /** The configuration field name, such as {@code flat_maximum} or {@code delay_reduction}. */
        public String getField() {
            return field;
        }

        /** The configured value (increases and reductions are fractions: 0.2 = 20 percent). */
        public double getValue() {
            return value;
        }

        /** True when the line helps the player (more Maximum Stamina, Regeneration, or Efficiency; less delay). */
        public boolean isBeneficial() {
            return beneficial;
        }
    }

    private final double swingCost;
    private final double drawPerSecond;
    private final double stability;
    private final double meleeShare;
    private final double projectileShare;
    private final double blockCostPerDamage;
    private final double weight;
    private final boolean weightApproximate;
    private final List<Modifier> modifiers;

    private ItemStaminaInfo(Builder b) {
        this.swingCost = b.swingCost;
        this.drawPerSecond = b.drawPerSecond;
        this.stability = b.stability;
        this.meleeShare = b.meleeShare;
        this.projectileShare = b.projectileShare;
        this.blockCostPerDamage = b.blockCostPerDamage;
        this.weight = b.weight;
        this.weightApproximate = b.weightApproximate;
        this.modifiers = Collections.unmodifiableList(new ArrayList<>(b.modifiers));
    }

    /** Stamina per melee swing, or {@code NaN} for items that are not melee weapons. */
    public double getSwingCost() {
        return swingCost;
    }

    /** Stamina per second while drawn, or {@code NaN} for items not drawn like a bow; 0 means free. */
    public double getDrawPerSecond() {
        return drawPerSecond;
    }

    /** True for blocking items (shields and anything that blocks like one). */
    public boolean isBlocking() {
        return !Double.isNaN(stability);
    }

    /** Stability, or {@code NaN} for items that do not block. */
    public double getStability() {
        return stability;
    }

    /** Fraction of a blocked melee hit the item stops (1.0 = all), or {@code NaN}. */
    public double getMeleeShare() {
        return meleeShare;
    }

    /** Fraction of a blocked projectile hit the item stops, or {@code NaN}. */
    public double getProjectileShare() {
        return projectileShare;
    }

    /** Block cost per point of incoming damage, or {@code NaN}. */
    public double getBlockCostPerDamage() {
        return blockCostPerDamage;
    }

    /** Weight when worn, or {@code NaN} for items that are not worn as armor. */
    public double getWeight() {
        return weight;
    }

    /** True when the weight is an estimate (Elenai rounds the whole worn set). */
    public boolean isWeightApproximate() {
        return weightApproximate;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public boolean isEmpty() {
        return Double.isNaN(swingCost) && Double.isNaN(drawPerSecond) && !isBlocking() && Double.isNaN(weight)
                && modifiers.isEmpty();
    }

    public static final class Builder {
        private double swingCost = Double.NaN;
        private double drawPerSecond = Double.NaN;
        private double stability = Double.NaN;
        private double meleeShare = Double.NaN;
        private double projectileShare = Double.NaN;
        private double blockCostPerDamage = Double.NaN;
        private double weight = Double.NaN;
        private boolean weightApproximate;
        private final List<Modifier> modifiers = new ArrayList<>();

        public Builder swingCost(double value) {
            swingCost = value;
            return this;
        }

        public Builder drawPerSecond(double value) {
            drawPerSecond = value;
            return this;
        }

        public Builder blocking(double stabilityValue, double melee, double projectile, double costPerDamage) {
            stability = stabilityValue;
            meleeShare = melee;
            projectileShare = projectile;
            blockCostPerDamage = costPerDamage;
            return this;
        }

        public Builder weight(double value, boolean approximate) {
            weight = value;
            weightApproximate = approximate;
            return this;
        }

        public Builder modifier(String field, double value, boolean beneficial) {
            modifiers.add(new Modifier(field, value, beneficial));
            return this;
        }

        public ItemStaminaInfo build() {
            return new ItemStaminaInfo(this);
        }
    }
}
