package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.Tags;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.CostCalculator;
import com.mahghuuuls.jass.core.StaminaPool;
import com.mahghuuuls.jass.core.StaminaProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * The only path that changes a player's Stamina. Applies, in order: game-mode exemption, the
 * start rule, the final cost, payment with the debt floor, and denial recording. Action hooks,
 * commands, and the API all come through here so these rules cannot diverge.
 */
public final class ActionGate {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    private static final int DEBUG_LINES_PER_SECOND = 4;
    private static final int TICKS_PER_SECOND = 20;

    private final StaminaSessionManager sessions;
    private final ProfileService profiles;

    ActionGate(StaminaSessionManager sessions, ProfileService profiles) {
        this.sessions = sessions;
        this.profiles = profiles;
    }

    /** True when the player spends no Stamina: players without a session, spectators, and (by default) Creative players. */
    public boolean isExempt(EntityPlayer player) {
        if (sessions.session(player) == null || player.isSpectator()) {
            return true;
        }
        return player.isCreative() && !ConfigModel.server().creativeConsumption();
    }

    /** True when the player may start a Stamina action now. */
    public boolean canStart(EntityPlayer player) {
        if (isExempt(player)) {
            return true;
        }
        StaminaSession session = sessions.session(player);
        return session != null && session.pool.canStart();
    }

    /**
     * Performs a Discrete Action: allowed only while Stamina is above zero, then pays the full
     * final cost (efficiency and minimum cost applied), which may create Stamina Debt.
     *
     * @return {@code true} if the action may happen; {@code false} if it was denied for Stamina
     */
    public boolean tryDiscrete(EntityPlayer player, ResourceLocation action, double baseCost) {
        if (isExempt(player)) {
            return true;
        }
        StaminaSession session = sessions.session(player);
        if (!session.pool.canStart()) {
            deny(player, session, action, DenialRecord.INSUFFICIENT_STAMINA);
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        ServerSettings settings = ConfigModel.server();
        double factors = CostCalculator.efficiencyFactor(settings.efficiencyScale(), profile.efficiency());
        double finalCost = CostCalculator.finalDiscreteCost(baseCost, factors, settings.minDiscreteCost());
        double floor = StaminaPool.debtFloor(profile.maximum(), settings.maxDebtFraction());
        session.pool.payDiscrete(finalCost, floor, profile.regenerationDelay());
        if (settings.debugLogging() && allowDebugLine(session, player.world.getTotalWorldTime())) {
            LOGGER.info("JASS spent player={} action={} cost={} internal={}",
                    player.getName(), action, format(finalCost), format(session.pool.stamina()));
        }
        return true;
    }

    /** The player's current state, or {@code null} for players without a session. */
    public StaminaReadout read(EntityPlayer player) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return null;
        }
        return new StaminaReadout(session.pool, isExempt(player), profiles.profile(player),
                session.lastDenial, session.denialCount);
    }

    /** Sets internal Stamina, clamped to the debt floor and maximum, and restarts the delay (operator and test use). */
    public void set(EntityPlayer player, double value) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return;
        }
        StaminaProfile profile = profiles.profile(player);
        ServerSettings settings = ConfigModel.server();
        double floor = StaminaPool.debtFloor(profile.maximum(), settings.maxDebtFraction());
        session.pool.set(value, floor, profile.maximum(), profile.regenerationDelay());
    }

    /** Fills Stamina to the player's maximum. */
    public void restore(EntityPlayer player) {
        StaminaSession session = sessions.session(player);
        if (session != null) {
            session.pool.restore(profiles.profile(player).maximum());
        }
    }

    /** Advances regeneration for one server tick. */
    void tick(EntityPlayer player, double seconds) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return;
        }
        StaminaProfile profile = profiles.profile(player);
        session.pool.tick(seconds, profile.regeneration(), profile.maximum());
    }

    private void deny(EntityPlayer player, StaminaSession session, ResourceLocation action, String reason) {
        long tick = player.world.getTotalWorldTime();
        session.lastDenial = new DenialRecord(action, reason, tick);
        session.denialCount++;
        if (ConfigModel.server().debugLogging() && allowDebugLine(session, tick)) {
            LOGGER.info("JASS denied player={} action={} reason={} internal={}",
                    player.getName(), action, reason, format(session.pool.stamina()));
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean allowDebugLine(StaminaSession session, long tick) {
        if (tick - session.debugWindowStartTick >= TICKS_PER_SECOND) {
            session.debugWindowStartTick = tick;
            session.debugLinesInWindow = 0;
        }
        if (session.debugLinesInWindow >= DEBUG_LINES_PER_SECOND) {
            return false;
        }
        session.debugLinesInWindow++;
        return true;
    }
}
