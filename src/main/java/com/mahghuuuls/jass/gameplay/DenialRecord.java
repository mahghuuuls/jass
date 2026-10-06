package com.mahghuuuls.jass.gameplay;

import net.minecraft.util.ResourceLocation;

/** The last action JASS refused for a player, with the reason and the world tick it happened. */
public final class DenialRecord {

    public static final String INSUFFICIENT_STAMINA = "insufficient_stamina";

    private final ResourceLocation action;
    private final String reason;
    private final long worldTick;

    DenialRecord(ResourceLocation action, String reason, long worldTick) {
        this.action = action;
        this.reason = reason;
        this.worldTick = worldTick;
    }

    public ResourceLocation action() {
        return action;
    }

    public String reason() {
        return reason;
    }

    public long worldTick() {
        return worldTick;
    }
}
