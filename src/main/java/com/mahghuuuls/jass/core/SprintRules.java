package com.mahghuuuls.jass.core;

/**
 * The single rule for when sprinting is a Stamina action. Both the server (which drains) and
 * the client (which refuses to start a sprint at zero) ask this, with the player's state as
 * plain flags, so the two sides cannot disagree.
 */
public final class SprintRules {

    private SprintRules() {
    }

    /** Sprinting costs Stamina only on land: not swimming or wading, riding, gliding, or flying. */
    public static boolean costsStamina(boolean sprinting, boolean inWater, boolean riding, boolean gliding,
            boolean flying) {
        return sprinting && !inWater && !riding && !gliding && !flying;
    }
}
