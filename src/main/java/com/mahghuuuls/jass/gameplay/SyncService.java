package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.network.JassNetwork;
import com.mahghuuuls.jass.network.StaminaSnapshotMessage;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Owns the client contract: one snapshot per player, sent when a value the client uses differs
 * from what was last sent, at most once every {@link #MIN_TICKS_BETWEEN_SENDS} ticks, and
 * immediately after login, respawn, and dimension changes. A change of "may spend" is sent at
 * once, so the client stops predicting actions (a held right click that retries a bow draw, a
 * sprint start) before the server has to refuse them. Comparing values means no caller has
 * to remember to flag a change.
 */
final class SyncService {

    static final int MIN_TICKS_BETWEEN_SENDS = 2;

    private final StaminaSessionManager sessions;
    private final ActionGate gate;

    SyncService(StaminaSessionManager sessions, ActionGate gate) {
        this.sessions = sessions;
        this.gate = gate;
    }

    void maybeSend(EntityPlayerMP player, long worldTick) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return;
        }
        StaminaReadout readout = gate.read(player);
        float visible = (float) readout.visible();
        float maximum = (float) readout.profile().maximum();
        boolean canSpend = readout.canSpend();
        int denialCount = readout.denialCount();
        boolean jumpCostEnabled = ConfigModel.server().jumpCostEnabled();
        boolean guardBroken = readout.guardBreakTicks() > 0;
        boolean gated = readout.gated();
        boolean changed = visible != session.lastSentVisible
                || maximum != session.lastSentMaximum
                || canSpend != session.lastSentCanSpend
                || denialCount != session.lastSentDenialCount
                || jumpCostEnabled != session.lastSentJumpCostEnabled
                || guardBroken != session.lastSentGuardBroken
                || gated != session.lastSentGated;
        boolean throttleOpen = worldTick - session.lastSyncTick >= MIN_TICKS_BETWEEN_SENDS
                || canSpend != session.lastSentCanSpend;
        if (!session.syncForced && !(changed && throttleOpen)) {
            return;
        }
        JassNetwork.channel().sendTo(new StaminaSnapshotMessage(visible, maximum, canSpend, denialCount, jumpCostEnabled,
                guardBroken, gated), player);
        session.syncForced = false;
        session.lastSyncTick = worldTick;
        session.lastSentVisible = visible;
        session.lastSentMaximum = maximum;
        session.lastSentCanSpend = canSpend;
        session.lastSentDenialCount = denialCount;
        session.lastSentJumpCostEnabled = jumpCostEnabled;
        session.lastSentGuardBroken = guardBroken;
        session.lastSentGated = gated;
    }
}
