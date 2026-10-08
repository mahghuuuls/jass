package com.mahghuuuls.jass.api.client;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posted on the Forge event bus (client) each frame before JASS draws its Stamina display, whatever
 * its own visibility rules decide. Cancel it to suppress every built-in display and draw your own
 * from the snapshot (REQ-085, ARC-013).
 */
@Cancelable
public final class StaminaHudRenderEvent extends Event {

    private final ClientStaminaSnapshot snapshot;
    private final float partialTicks;
    private final int scaledWidth;
    private final int scaledHeight;

    public StaminaHudRenderEvent(ClientStaminaSnapshot snapshot, float partialTicks, int scaledWidth,
            int scaledHeight) {
        this.snapshot = snapshot;
        this.partialTicks = partialTicks;
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;
    }

    public ClientStaminaSnapshot getSnapshot() {
        return snapshot;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public int getScaledWidth() {
        return scaledWidth;
    }

    public int getScaledHeight() {
        return scaledHeight;
    }
}
