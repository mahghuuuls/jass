package com.mahghuuuls.jass.client;

/**
 * Decides whether Elenai Dodge 2's armor weight tooltip is hidden (REQ-073); asked by the optional
 * Elenai tooltip Mixin. Hidden while the server takes Effective Weight from Elenai, so armor shows
 * only JASS's weight line, the same presentation as without Elenai.
 */
public final class ElenaiWeightTooltip {

    private static volatile boolean elenaiWeightSource;

    private ElenaiWeightTooltip() {
    }

    /** Set from the server's item values (REV-010); false before they arrive and after leaving. */
    static void setElenaiWeightSource(boolean value) {
        elenaiWeightSource = value;
    }

    /** True while the server uses Elenai's weight and the player keeps the default option. */
    public static boolean hide() {
        return JassClientConfig.hideElenaiWeightTooltip && elenaiWeightSource;
    }
}
