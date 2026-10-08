package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.core.StaminaPool;
import com.mahghuuuls.jass.core.StaminaProfile;

/** A read-only copy of one player's Stamina state and profile, for inspection and sync. */
public final class StaminaReadout {

    private final double internal;
    private final double visible;
    private final double debt;
    private final boolean poolCanStart;
    private final double delayRemaining;
    private final boolean exempt;
    private final StaminaProfile profile;
    private final DenialRecord lastDenial;
    private final int denialCount;
    private final int guardBreakTicks;

    StaminaReadout(StaminaPool pool, boolean exempt, StaminaProfile profile, DenialRecord lastDenial,
            int denialCount, int guardBreakTicks) {
        this.internal = pool.stamina();
        this.visible = pool.visible();
        this.debt = pool.debt();
        this.poolCanStart = pool.canStart();
        this.delayRemaining = pool.delayRemaining();
        this.exempt = exempt;
        this.profile = profile;
        this.lastDenial = lastDenial;
        this.denialCount = denialCount;
        this.guardBreakTicks = guardBreakTicks;
    }

    public double internal() {
        return internal;
    }

    public double visible() {
        return visible;
    }

    public double debt() {
        return debt;
    }

    public double delayRemaining() {
        return delayRemaining;
    }

    public boolean exempt() {
        return exempt;
    }

    /** True when the player may start a Stamina action now. */
    public boolean canSpend() {
        return exempt || poolCanStart;
    }

    public StaminaProfile profile() {
        return profile;
    }

    /** The last action JASS refused, or {@code null} if none since login. */
    public DenialRecord lastDenial() {
        return lastDenial;
    }

    /** Server ticks of Guard Break left; 0 when none. */
    public int guardBreakTicks() {
        return guardBreakTicks;
    }

    /** Number of denials since login; the client flashes when it changes. */
    public int denialCount() {
        return denialCount;
    }
}
