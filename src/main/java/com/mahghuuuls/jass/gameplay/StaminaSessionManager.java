package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds every online player's session in memory, keyed by UUID so it survives the entity being
 * replaced (leaving the End) and needs no copying. Owns the lifecycle rules: created full on
 * login, refilled on respawn, kept across dimension changes, dropped on logout. Only logged-in
 * players have a session, so fake players never get one.
 */
public final class StaminaSessionManager {

    private final Map<UUID, StaminaSession> sessions = new HashMap<>();
    private final ProfileService profiles;

    StaminaSessionManager(ProfileService profiles) {
        this.profiles = profiles;
    }

    /**
     * The player's session, or {@code null} when the player has none. Never creates one. A fake
     * player built from a real player's profile shares that UUID, so it is excluded here rather
     * than borrowing the owner's Stamina.
     */
    StaminaSession session(EntityPlayer player) {
        if (player instanceof FakePlayer) {
            return null;
        }
        return sessions.get(player.getUniqueID());
    }

    void onLogin(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return;
        }
        sessions.put(player.getUniqueID(), new StaminaSession(profiles.profile(player).maximum()));
    }

    void onRespawn(EntityPlayer player) {
        StaminaSession session = session(player);
        if (session != null) {
            session.pool.restore(profiles.profile(player).maximum());
            session.guardBreakTicks = 0;
            session.syncForced = true;
        }
    }

    void onDimensionChanged(EntityPlayer player) {
        StaminaSession session = session(player);
        if (session != null) {
            session.syncForced = true;
        }
    }

    void onLogout(EntityPlayer player) {
        sessions.remove(player.getUniqueID());
    }

    void clear() {
        sessions.clear();
    }
}
