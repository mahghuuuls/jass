package com.mahghuuuls.jass.api.client;

/** What the local player's client last received from the server. Read-only. */
public final class ClientStaminaSnapshot {

    public static final ClientStaminaSnapshot NONE = new ClientStaminaSnapshot(false, 0.0F, 0.0F, true, false, false);

    private final boolean received;
    private final float visible;
    private final float maximum;
    private final boolean canSpend;
    private final boolean gated;
    private final boolean guardBroken;

    public ClientStaminaSnapshot(boolean received, float visible, float maximum, boolean canSpend, boolean gated,
            boolean guardBroken) {
        this.received = received;
        this.visible = visible;
        this.maximum = maximum;
        this.canSpend = canSpend;
        this.gated = gated;
        this.guardBroken = guardBroken;
    }

    /** False until the first snapshot arrives after joining. */
    public boolean isReceived() {
        return received;
    }

    /** Visible Stamina (never negative). */
    public float getVisibleStamina() {
        return visible;
    }

    public float getMaximumStamina() {
        return maximum;
    }

    /** True while the player may start a Stamina action. */
    public boolean canSpend() {
        return canSpend;
    }

    /** True while Inhibited gating suspends costs. */
    public boolean isGated() {
        return gated;
    }

    public boolean isGuardBroken() {
        return guardBroken;
    }
}
