package com.mahghuuuls.jass.gameplay;

/** Which cost factors apply to an action (REQ-013). */
public enum CostKind {

    /** Melee, bow draw, shield block: the efficiency factor only. */
    STANDARD,

    /** Sprint, Elenai dodge, jump: the efficiency factor and the weight multiplier. */
    MOVEMENT
}
