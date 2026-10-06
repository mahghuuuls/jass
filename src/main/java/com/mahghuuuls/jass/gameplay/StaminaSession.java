package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.core.StaminaPool;

/**
 * Short-term Stamina state of one online player. Never saved. The pool and denial fields are
 * changed only through {@link ActionGate} and {@link StaminaSessionManager}; the sync fields
 * belong to {@link SyncService}.
 */
final class StaminaSession {

    final StaminaPool pool;

    DenialRecord lastDenial;
    /** Increases on every denial; the client flashes when it sees it change. */
    int denialCount;
    long debugWindowStartTick = -20L;
    int debugLinesInWindow;

    boolean syncForced = true;
    long lastSyncTick = -SyncService.MIN_TICKS_BETWEEN_SENDS;
    float lastSentVisible = Float.NaN;
    float lastSentMaximum = Float.NaN;
    boolean lastSentCanSpend;
    int lastSentDenialCount;

    StaminaSession(double initialStamina) {
        this.pool = new StaminaPool(initialStamina);
    }
}
