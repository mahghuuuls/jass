package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.network.JassNetwork;
import com.mahghuuuls.jass.network.StaminaSnapshotMessage;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Owns the client contract: one snapshot per player, sent when a value the client uses differs
 * from what was last sent, at most once every {@link #MIN_TICKS_BETWEEN_SENDS} ticks, and
 * immediately after login, respawn, and dimension changes. Comparing values means no caller has
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
        boolean changed = visible != session.lastSentVisible
                || maximum != session.lastSentMaximum
                || canSpend != session.lastSentCanSpend
                || denialCount != session.lastSentDenialCount;
        boolean throttleOpen = worldTick - session.lastSyncTick >= MIN_TICKS_BETWEEN_SENDS;
        if (!session.syncForced && !(changed && throttleOpen)) {
            return;
        }
        JassNetwork.channel().sendTo(new StaminaSnapshotMessage(visible, maximum, canSpend, denialCount), player);
        session.syncForced = false;
        session.lastSyncTick = worldTick;
        session.lastSentVisible = visible;
        session.lastSentMaximum = maximum;
        session.lastSentCanSpend = canSpend;
        session.lastSentDenialCount = denialCount;
    }
}
