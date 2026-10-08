package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posted on the Forge event bus (server) with the final cost of any JASS action, just before it is
 * paid (REQ-083). Listeners may change the cost or cancel the event, which makes the action free.
 * For a Continuous Action the cost is that tick's amount.
 */
@Cancelable
public final class StaminaCostEvent extends Event {

    private final EntityPlayer player;
    private final ResourceLocation action;
    private double cost;

    public StaminaCostEvent(EntityPlayer player, ResourceLocation action, double cost) {
        this.player = player;
        this.action = action;
        this.cost = cost;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getAction() {
        return action;
    }

    public double getCost() {
        return cost;
    }

    /** Sets the cost; values below 0 count as 0. The cost must be a finite number. */
    public void setCost(double cost) {
        if (Double.isNaN(cost) || Double.isInfinite(cost)) {
            throw new IllegalArgumentException("cost must be a finite number");
        }
        this.cost = Math.max(0.0, cost);
    }
}
