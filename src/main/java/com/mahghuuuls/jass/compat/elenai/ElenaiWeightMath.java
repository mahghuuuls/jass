package com.mahghuuuls.jass.compat.elenai;

/**
 * Elenai Dodge 2 1.1.0's armor weight arithmetic, reimplemented as plain math from its documented
 * behavior and decompiled reference (REQ-071, D9; no code copied) so it can be
 * tested without Elenai: per piece the configured override or {@code (armor points / 2) x 1.8}
 * with integer division; the sum floored, minus Lightweight enchantment levels, then rounded down
 * to an even number unless Half Feathers is on. JASS then converts and never lets it go negative.
 */
final class ElenaiWeightMath {

    private ElenaiWeightMath() {
    }

    /** Weight of one armor piece without an override, as Elenai's client computes it. */
    static double pieceFromArmorPoints(int armorPoints) {
        return (armorPoints / 2) * 1.8D;
    }

    /** Elenai's integer weight from the summed piece weights. */
    static int elenaiWeight(double pieceSum, int lightweightLevels, boolean halfFeathers) {
        int weight = (int) Math.floor(pieceSum) - lightweightLevels;
        return halfFeathers ? weight : (weight / 2) * 2;
    }

    /** JASS Effective Weight from Elenai's weight: converted and never negative. */
    static double effectiveWeight(int elenaiWeight, double conversion) {
        return Math.max(0.0, elenaiWeight * Math.max(0.0, conversion));
    }
}
