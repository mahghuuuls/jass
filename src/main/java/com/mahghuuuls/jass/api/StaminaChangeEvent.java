package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posted on the Forge event bus (server) after a spend, drain, restore, or set changed a player's
 * Stamina (REQ-084). Regeneration does not post it.
 */
public final class StaminaChangeEvent extends Event {

    private final EntityPlayer player;
    private final StaminaPublicState oldState;
    private final StaminaPublicState newState;
    private final ResourceLocation cause;

    public StaminaChangeEvent(EntityPlayer player, StaminaPublicState oldState, StaminaPublicState newState,
            ResourceLocation cause) {
        this.player = player;
        this.oldState = oldState;
        this.newState = newState;
        this.cause = cause;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public StaminaPublicState getOldState() {
        return oldState;
    }

    public StaminaPublicState getNewState() {
        return newState;
    }

    public ResourceLocation getCause() {
        return cause;
    }
}
