package com.mahghuuuls.jass.gameplay.hooks;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.CostKind;
import com.mahghuuuls.jass.gameplay.ItemKeys;
import com.mahghuuuls.jass.gameplay.JassActions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Drains Stamina for each server tick a player holds a bow-draw item in use (any item whose use
 * action is bow drawing), refuses to start a draw at zero, and ends a draw without firing when
 * Stamina runs out. An item whose rate is 0 is never drained or interrupted, but like every
 * Stamina action it cannot start at zero.
 */
public final class BowHook {

    /**
     * Vanilla retries a held right click every 4 ticks; packets can arrive further apart. Refusals
     * closer together than this, and a refusal right after a draw ran out, are one held attempt
     * and record one denial, so holding a bow at zero flashes once.
     */
    private static final long HELD_RETRY_GAP_TICKS = 10L;

    private final ActionGate gate;
    /** Server thread only; entries go away with the player entity. */
    private final Map<EntityPlayer, Long> lastRefusedTick = new WeakHashMap<>();

    public BowHook(ActionGate gate) {
        this.gate = gate;
    }

    /** LOWEST, so only draws that would really start are refused and counted as denials. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote
                || !isBowDraw(event.getItem())) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (gate.canStart(player)) {
            return;
        }
        event.setCanceled(true);
        long now = player.world.getTotalWorldTime();
        Long last = lastRefusedTick.put(player, now);
        if (last == null || now - last > HELD_RETRY_GAP_TICKS) {
            gate.reportRefusedAttempt(player, JassActions.BOW);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.START || player.world.isRemote || !player.isHandActive()) {
            return;
        }
        ItemStack stack = player.getActiveItemStack();
        if (!isBowDraw(stack)) {
            return;
        }
        double rate = ConfigModel.server().bowDrawCost(ItemKeys.registryName(stack), ItemKeys.metadata(stack));
        if (rate > 0.0 && !gate.drainContinuous(player, JassActions.BOW, rate, CostKind.STANDARD)) {
            // Clears the draw without the release path, so no arrow is fired or used up. The gate
            // recorded the denial; a held right click retrying next is the same attempt.
            player.resetActiveHand();
            lastRefusedTick.put(player, player.world.getTotalWorldTime());
        }
    }

    /** The definition of a bow-draw item (REQ-032); the client guard uses the same test. */
    private static boolean isBowDraw(ItemStack stack) {
        return !stack.isEmpty() && stack.getItemUseAction() == EnumAction.BOW;
    }
}
