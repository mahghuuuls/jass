package com.mahghuuuls.jass.api;

/** A read-only copy of one player's Stamina at one moment. */
public final class StaminaPublicState {

    private final double stamina;
    private final double maximum;

    public StaminaPublicState(double stamina, double maximum) {
        this.stamina = stamina;
        this.maximum = maximum;
    }

    /** Internal Stamina; below zero is Stamina Debt. */
    public double getStamina() {
        return stamina;
    }

    /** Stamina clamped at zero, as the HUD shows it. */
    public double getVisibleStamina() {
        return Math.max(0.0, stamina);
    }

    /** Stamina Debt as a positive number, or 0. */
    public double getDebt() {
        return Math.max(0.0, -stamina);
    }

    public double getMaximumStamina() {
        return maximum;
    }
}
