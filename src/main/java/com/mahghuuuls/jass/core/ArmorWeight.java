package com.mahghuuuls.jass.core;

/**
 * Standalone weight of one worn armor item (REQ-012): its configured weight when the item has
 * one, otherwise its armor points times the fallback weight per armor point, never negative.
 */
public final class ArmorWeight {

    private ArmorWeight() {
    }

    /**
     * @param configuredWeight the item's configured weight, or {@code NaN} when it has none
     * @param armorPoints      the armor points the item gives in the slot it is worn in
     */
    public static double itemWeight(double configuredWeight, double armorPoints, double fallbackPerArmorPoint) {
        if (!Double.isNaN(configuredWeight)) {
            return Math.max(0.0, configuredWeight);
        }
        return Math.max(0.0, armorPoints) * Math.max(0.0, fallbackPerArmorPoint);
    }
}
