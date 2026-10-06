package com.mahghuuuls.jass.core;

/**
 * One player's Stamina: the internal value, which may be negative (Stamina Debt), and the
 * Regeneration Delay countdown. Owns every rule about how the value moves: debt floor,
 * discrete payment, continuous drain that stops at zero, regeneration that repays debt first,
 * and clamping when the maximum drops.
 *
 * <p>Plain Java on purpose: callers pass the current stats in, so the rules can be tested
 * without Minecraft.
 */
public final class StaminaPool {

    private double stamina;
    private double delayRemaining;
    private boolean drainedSinceLastTick;

    public StaminaPool(double initialStamina) {
        this.stamina = initialStamina;
    }

    public double stamina() {
        return stamina;
    }

    /** The value the player sees: internal Stamina with debt shown as zero. */
    public double visible() {
        return Math.max(0.0, stamina);
    }

    /** The debt to repay before the visible value rises, or zero. */
    public double debt() {
        return Math.max(0.0, -stamina);
    }

    public double delayRemaining() {
        return delayRemaining;
    }

    /** Any action may start only while Stamina is above zero. */
    public boolean canStart() {
        return stamina > 0.0;
    }

    /**
     * Pays the full final cost of a Discrete Action that was allowed to start. The result may
     * be negative, but never below {@code debtFloor}.
     */
    public void payDiscrete(double finalCost, double debtFloor, double delaySeconds) {
        if (finalCost <= 0.0) {
            return;
        }
        stamina = Math.max(stamina - finalCost, debtFloor);
        delayRemaining = delaySeconds;
    }

    /**
     * Drains up to {@code amount} for one tick of a Continuous Action. Never goes below zero
     * and never drains while Stamina is already zero or below.
     *
     * @return the amount actually drained
     */
    public double drainContinuous(double amount, double delaySeconds) {
        if (amount <= 0.0 || stamina <= 0.0) {
            return 0.0;
        }
        double drained = Math.min(amount, stamina);
        stamina -= drained;
        delayRemaining = delaySeconds;
        drainedSinceLastTick = true;
        return drained;
    }

    /**
     * Advances regeneration by {@code seconds}. A tick in which a Continuous Action drained
     * regenerates nothing; otherwise the delay counts down first and any time left over
     * regenerates. Positive Stamina above {@code maximum} is clamped; debt is kept.
     */
    public void tick(double seconds, double regenerationPerSecond, double maximum) {
        if (stamina > maximum) {
            stamina = maximum;
        }
        if (drainedSinceLastTick) {
            drainedSinceLastTick = false;
            return;
        }
        double regenSeconds = seconds;
        if (delayRemaining > 0.0) {
            if (delayRemaining >= seconds) {
                delayRemaining -= seconds;
                return;
            }
            regenSeconds = seconds - delayRemaining;
            delayRemaining = 0.0;
        }
        if (stamina < maximum) {
            stamina = Math.min(maximum, stamina + regenerationPerSecond * regenSeconds);
        }
    }

    /** Sets the internal value within {@code [debtFloor, maximum]} and restarts the delay. */
    public void set(double value, double debtFloor, double maximum, double delaySeconds) {
        stamina = Math.min(maximum, Math.max(debtFloor, value));
        delayRemaining = delaySeconds;
    }

    /** Fills Stamina to {@code maximum} and clears the delay. */
    public void restore(double maximum) {
        stamina = maximum;
        delayRemaining = 0.0;
        drainedSinceLastTick = false;
    }

    /**
     * The lowest internal Stamina allowed: {@code -(fraction x maximum)}, or no floor when the
     * fraction is negative (uncapped debt).
     */
    public static double debtFloor(double maximum, double maxDebtFraction) {
        if (maxDebtFraction < 0.0) {
            return Double.NEGATIVE_INFINITY;
        }
        return -maxDebtFraction * maximum;
    }
}
