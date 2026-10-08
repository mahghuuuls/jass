package com.mahghuuuls.jass.gameplay.hooks;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.CostKind;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Charges ground jumps the client reports, when jump cost is enabled (REQ-034). The server never
 * charges on its own, so upward knockback is never a jump. A report counts only if the server saw
 * the player on the ground a moment ago, the player is not swimming, in lava, riding, or flying,
 * and the last charged jump was not too recent, which bounds a flood of fake reports.
 */
public final class JumpHook implements JassNetwork.ServerJumpSink {

    /** Movement and the jump report arrive in different server ticks; allow this much lag. */
    private static final long GROUND_GRACE_TICKS = 5L;
    /** No real ground jump follows the previous one this fast, even onto a step. */
    private static final long MIN_TICKS_BETWEEN_JUMPS = 5L;

    private final ActionGate gate;
    /** Server thread only; entries go away with the player entity. */
    private final Map<EntityPlayer, JumpState> states = new WeakHashMap<>();

    public JumpHook(ActionGate gate) {
        this.gate = gate;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase == TickEvent.Phase.END && !player.world.isRemote && player.onGround) {
            state(player).lastGroundTick = player.world.getTotalWorldTime();
        }
    }

    @Override
    public void onJump(EntityPlayerMP player) {
        ServerSettings settings = ConfigModel.server();
        if (!settings.jumpCostEnabled() || player.isInWater() || player.isInLava() || player.isRiding()
                || player.capabilities.isFlying || player.isElytraFlying()) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        JumpState state = state(player);
        // The landing packet arrives before the jump report, so onGround may already be true even
        // when no server tick has recorded it yet (a held jump leaves one client tick after landing).
        boolean recentlyOnGround = player.onGround || now - state.lastGroundTick <= GROUND_GRACE_TICKS;
        if (!recentlyOnGround || now - state.lastChargedTick < MIN_TICKS_BETWEEN_JUMPS) {
            return;
        }
        state.lastChargedTick = now;
        gate.tryDiscrete(player, JassActions.JUMP, settings.jumpCost(), CostKind.MOVEMENT);
    }

    private JumpState state(EntityPlayer player) {
        return states.computeIfAbsent(player, p -> new JumpState());
    }

    private static final class JumpState {
        long lastGroundTick = Long.MIN_VALUE / 2;
        long lastChargedTick = Long.MIN_VALUE / 2;
    }
}
