package com.mahghuuuls.jass.api;

/** The answer to preparing an action (REQ-088). */
public enum PreparationResult {
    /** Not allowed now (Available Stamina 0 or below, or no Stamina session). Do not run the effect. */
    REFUSED,
    /** Allowed and costs nothing: costs are suspended (Creative, spectator, Inhibited gating) or the final cost is 0. */
    FREE,
    /** Allowed; a Stamina Claim holds the final cost until settle or release. */
    ACCEPTED
}
