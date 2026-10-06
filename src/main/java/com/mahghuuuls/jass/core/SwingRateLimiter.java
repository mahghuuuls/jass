package com.mahghuuuls.jass.core;

/**
 * Bounds how often client-reported air swings are charged. Vanilla lets a Survival player miss
 * at most once every {@code intervalTicks}. With a burst of 1 no window ever holds more charges
 * than vanilla allows; a report that arrives early through network jitter is dropped, which
 * can only undercharge.
 */
public final class SwingRateLimiter {

    private final long intervalTicks;
    private final long capacity;
    private long credit;
    private long lastTick;
    private boolean started;

    public SwingRateLimiter(int intervalTicks, int burst) {
        this.intervalTicks = intervalTicks;
        this.capacity = (long) intervalTicks * burst;
        this.credit = capacity;
    }

    /** Accepts one report at {@code tick} if the rate allows it. */
    public boolean tryAccept(long tick) {
        if (started) {
            credit = Math.min(capacity, credit + Math.max(0L, tick - lastTick));
        }
        started = true;
        lastTick = tick;
        if (credit >= intervalTicks) {
            credit -= intervalTicks;
            return true;
        }
        return false;
    }
}
