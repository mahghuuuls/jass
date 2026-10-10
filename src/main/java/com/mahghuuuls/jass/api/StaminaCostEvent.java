package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
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
    private final ItemStack stack;
    private final EnumHand hand;
    private double cost;

    public StaminaCostEvent(EntityPlayer player, ResourceLocation action, double cost) {
        this(player, action, cost, ItemStack.EMPTY, null);
    }

    /** With the item and hand behind the action (REQ-087). */
    public StaminaCostEvent(EntityPlayer player, ResourceLocation action, double cost, ItemStack stack, EnumHand hand) {
        this.player = player;
        this.action = action;
        this.cost = cost;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.hand = hand;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getAction() {
        return action;
    }

    /**
     * The item behind the action: the swinging item for {@code jass:melee} (the off-hand item for an
     * RLCombat off-hand attack), the drawn item for {@code jass:bow}, the blocking item for
     * {@code jass:block}; {@link ItemStack#EMPTY} for actions without an item (sprint, jump, dodge).
     * This is the live stack: read it, never change it.
     */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * The hand behind the action, or {@code null} for actions without an item. Melee always has a hand,
     * also when it is empty (an empty-hand swing gives {@code MAIN_HAND} and {@link ItemStack#EMPTY}).
     */
    public EnumHand getHand() {
        return hand;
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
