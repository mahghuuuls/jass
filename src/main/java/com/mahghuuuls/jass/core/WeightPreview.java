package com.mahghuuuls.jass.core;

/**
 * One armor piece's weight for item tooltips (REQ-058). The standalone weight is exact (it is the
 * same per-piece rule the server sums). Elenai Dodge 2's piece weight is only an estimate: Elenai
 * rounds the whole worn set (floor, Lightweight, even numbers), so tooltips label it "about".
 */
public final class WeightPreview {

    /** Elenai's weight per two armor points for armor without an override. */
    private static final double ELENAI_WEIGHT_PER_TWO_POINTS = 1.8;

    private WeightPreview() {
    }

    /** Standalone weight: the configured weight, or armor points times the fallback (REQ-012). */
    public static double standalone(double configuredWeight, double armorPoints, double fallbackPerArmorPoint) {
        return ArmorWeight.itemWeight(configuredWeight, armorPoints, fallbackPerArmorPoint);
    }

    /** Elenai's weight of one piece without an override: (armor points / 2, rounded down) x 1.8. */
    public static double elenaiFromArmorPoints(int armorPoints) {
        return (armorPoints / 2) * ELENAI_WEIGHT_PER_TWO_POINTS;
    }

    /**
     * Elenai's estimate for one piece in JASS weight.
     *
     * @param overrideWeight the sum of Elenai override entries for this item, or {@code NaN} when none
     * @param isArmor        true for armor items, which use the armor formula without an override
     * @param armorPoints    the item's armor points
     */
    public static double elenaiPiece(double overrideWeight, boolean isArmor, int armorPoints, double conversion) {
        double elenai = !Double.isNaN(overrideWeight) ? overrideWeight : isArmor ? elenaiFromArmorPoints(armorPoints) : 0.0;
        return Math.max(0.0, elenai * Math.max(0.0, conversion));
    }
}
