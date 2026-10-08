package com.mahghuuuls.jass.api;

import net.minecraft.util.ResourceLocation;

/** What an API spend, restore, set, or action did. */
public final class StaminaMutationResult {

    private final boolean successful;
    private final double requested;
    private final ResourceLocation cause;
    private final StaminaPublicState oldState;
    private final StaminaPublicState newState;

    public StaminaMutationResult(boolean successful, double requested, ResourceLocation cause,
            StaminaPublicState oldState, StaminaPublicState newState) {
        this.successful = successful;
        this.requested = requested;
        this.cause = cause;
        this.oldState = oldState;
        this.newState = newState;
    }

    /** False when the action was refused (Stamina 0 or below) or the player has no Stamina session. */
    public boolean isSuccessful() {
        return successful;
    }

    public double getRequestedAmount() {
        return requested;
    }

    /** New minus old internal Stamina; 0 when nothing changed. */
    public double getActualDelta() {
        return oldState == null || newState == null ? 0.0 : newState.getStamina() - oldState.getStamina();
    }

    public ResourceLocation getCause() {
        return cause;
    }

    /** State before; {@code null} for a player without a session. */
    public StaminaPublicState getOldState() {
        return oldState;
    }

    /** State after; {@code null} for a player without a session. */
    public StaminaPublicState getNewState() {
        return newState;
    }
}
