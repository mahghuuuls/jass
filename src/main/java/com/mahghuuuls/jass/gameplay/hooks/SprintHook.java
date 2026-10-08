package com.mahghuuuls.jass.gameplay.hooks;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.core.SprintRules;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.CostKind;
import com.mahghuuuls.jass.api.JassActions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Drains Stamina for each server tick a player sprints where sprinting costs Stamina, and stops
 * the sprint on the server on the tick Stamina reaches zero (the client then refuses to start a
 * sprint on its own until it may spend again). Where sprinting is free is decided by
 * {@link SprintRules}.
 */
public final class SprintHook {

    private final ActionGate gate;

    public SprintHook(ActionGate gate) {
        this.gate = gate;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.START || player.world.isRemote || !costsStamina(player)) {
            return;
        }
        if (!gate.drainContinuous(player, JassActions.SPRINT, ConfigModel.server().sprintCost(), CostKind.MOVEMENT)) {
            player.setSprinting(false);
        }
    }

    private static boolean costsStamina(EntityPlayer player) {
        return SprintRules.costsStamina(player.isSprinting(), player.isInWater(), player.isRiding(),
                player.isElytraFlying(), player.capabilities.isFlying);
    }
}
