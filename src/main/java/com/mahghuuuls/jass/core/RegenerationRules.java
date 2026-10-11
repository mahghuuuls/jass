package com.mahghuuuls.jass.core;

/** The Stamina Regeneration rate that applies right now (REQ-004, REV-015). */
public final class RegenerationRules {

    private RegenerationRules() {
    }

    /**
     * The full rate, or {@code raisedShare} of it (limited to 0..1) while a blocking item is raised.
     * A share that is not a number leaves the rate unchanged.
     */
    public static double rate(double regeneration, boolean blockingItemRaised, double raisedShare) {
        if (!blockingItemRaised || Double.isNaN(raisedShare)) {
            return regeneration;
        }
        return regeneration * Math.min(1.0, Math.max(0.0, raisedShare));
    }
}
