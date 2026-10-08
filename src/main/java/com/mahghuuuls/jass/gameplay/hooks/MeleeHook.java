package com.mahghuuuls.jass.gameplay.hooks;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.core.SwingRateLimiter;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.AttackHandSource;
import com.mahghuuuls.jass.gameplay.ItemKeys;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Charges committed melee swings: entity attacks seen on the server, and air swings the client
 * reports. Block clicks never reach either path, so mining is never charged. Owns only the
 * mechanism (which item, whether a report is plausible); costs and denial belong to the gate.
 */
public final class MeleeHook implements JassNetwork.ServerInputSink {

    /** Vanilla lets a Survival player swing at air at most once every 10 ticks. */
    private static final int VANILLA_MISS_INTERVAL_TICKS = 10;
    private static final int MISS_BURST = 1;

    private final ActionGate gate;
    private final AttackHandSource hands;
    private final Map<EntityPlayer, SwingRateLimiter> airSwingLimiters = new WeakHashMap<>();

    public MeleeHook(ActionGate gate, AttackHandSource hands) {
        this.gate = gate;
        this.hands = hands;
    }

    /**
     * Runs last so an attack another mod cancels is never charged. A listener that cancels at
     * LOWEST priority after this one runs cannot be ruled out; such an attack is charged.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) {
            return;
        }
        ItemStack stack = player.getHeldItem(hands.handOf(event));
        if (!gate.tryDiscrete(player, JassActions.MELEE, meleeCost(stack))) {
            event.setCanceled(true);
        }
    }

    @Override
    public void onAirSwing(EntityPlayerMP player) {
        SwingRateLimiter limiter = airSwingLimiters.computeIfAbsent(player,
                p -> new SwingRateLimiter(VANILLA_MISS_INTERVAL_TICKS, MISS_BURST));
        if (!limiter.tryAccept(player.world.getTotalWorldTime())) {
            return;
        }
        gate.tryDiscrete(player, JassActions.MELEE, meleeCost(player.getHeldItemMainhand()));
    }

    @Override
    public void onRefusedAttack(EntityPlayerMP player) {
        gate.reportRefusedAttempt(player, JassActions.MELEE);
    }

    private static double meleeCost(ItemStack stack) {
        return ConfigModel.server().meleeCost(ItemKeys.registryName(stack), ItemKeys.metadata(stack));
    }
}
