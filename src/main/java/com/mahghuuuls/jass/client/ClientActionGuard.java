package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.network.AirSwingMessage;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client-side prediction of Stamina rules from the last snapshot, so the local player does not
 * see an action the server is about to refuse, and reporting of facts only the client sees.
 * The server stays authoritative.
 */
public final class ClientActionGuard {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer().world.isRemote && !ClientStaminaState.canSpend()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // The server cannot see misses; it validates the report and decides the cost.
        JassNetwork.channel().sendToServer(new AirSwingMessage());
    }
}
