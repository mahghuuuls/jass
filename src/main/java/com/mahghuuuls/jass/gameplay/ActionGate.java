package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.Tags;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.api.StaminaChangeEvent;
import com.mahghuuuls.jass.api.StaminaCostEvent;
import com.mahghuuuls.jass.api.StaminaPublicState;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.CostCalculator;
import com.mahghuuuls.jass.core.StaminaPool;
import com.mahghuuuls.jass.core.StaminaProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * The only path that changes a player's Stamina. Applies, in order: game-mode exemption and
 * integration gating (both suspend costs and denials, never regeneration), the
 * start rule, the final cost, payment with the debt floor, and denial recording. Action hooks,
 * commands, and the API all come through here so these rules cannot diverge.
 */
public final class ActionGate {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    private static final int DEBUG_LINES_PER_SECOND = 4;
    private static final int TICKS_PER_SECOND = 20;
    private static final double SECONDS_PER_TICK = 1.0 / TICKS_PER_SECOND;

    private final StaminaSessionManager sessions;
    private final ProfileService profiles;

    ActionGate(StaminaSessionManager sessions, ProfileService profiles) {
        this.sessions = sessions;
        this.profiles = profiles;
    }

    private volatile GatingSource gating = GatingSource.NONE;

    /** Wires an integration that can suspend costs (Inhibited). */
    public void useGating(GatingSource source) {
        this.gating = source;
    }

    /** True while an integration suspends this player's costs (Inhibited without its effect). */
    public boolean gated(EntityPlayer player) {
        return gating.suspended(player);
    }

    /** True when actions cost this player nothing and are never denied for Stamina right now. */
    private boolean costsSuspended(EntityPlayer player) {
        return isExempt(player) || gated(player);
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
        if (costsSuspended(player)) {
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
        return tryDiscrete(player, action, baseCost, CostKind.STANDARD);
    }

    /** As {@link #tryDiscrete(EntityPlayer, ResourceLocation, double)}, with the weight multiplier for a Movement Action. */
    public boolean tryDiscrete(EntityPlayer player, ResourceLocation action, double baseCost, CostKind kind) {
        if (costsSuspended(player)) {
            return true;
        }
        StaminaSession session = sessions.session(player);
        if (refuseIfCannotStart(player, session, action)) {
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        ServerSettings settings = ConfigModel.server();
        double factors = factors(kind, profile, settings);
        double finalCost = participate(player, action,
                CostCalculator.finalDiscreteCost(baseCost, factors, settings.minDiscreteCost()));
        double floor = StaminaPool.debtFloor(profile.maximum(), settings.maxDebtFraction());
        double before = session.pool.stamina();
        session.pool.payDiscrete(finalCost, floor, profile.regenerationDelay());
        notifyChange(player, before, session.pool.stamina(), profile.maximum(), action);
        if (settings.debugLogging() && allowDebugLine(session, player.world.getTotalWorldTime())) {
            LOGGER.info("JASS spent player={} action={} cost={} internal={}",
                    player.getName(), action, format(finalCost), format(session.pool.stamina()));
        }
        return true;
    }

    /**
     * Drains one server tick of a Continuous Action. Allowed only while Stamina is above zero;
     * drains at most down to zero (never into debt) and pauses regeneration for the tick. The tick
     * that reaches zero records one denial and returns {@code false}, so the action stops on that
     * tick, on the server, before the client can stop it on its own (which would leave no denial).
     *
     * @return {@code true} if the action may continue; {@code false} if Stamina is gone and the
     *         caller must stop it (a denial is recorded)
     */
    public boolean drainContinuous(EntityPlayer player, ResourceLocation action, double costPerSecond, CostKind kind) {
        if (costsSuspended(player)) {
            return true;
        }
        StaminaSession session = sessions.session(player);
        if (refuseIfCannotStart(player, session, action)) {
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        ServerSettings settings = ConfigModel.server();
        double factors = factors(kind, profile, settings);
        double before = session.pool.stamina();
        double drained = session.pool.drainContinuous(
                participate(player, action, CostCalculator.continuousCost(costPerSecond, factors, SECONDS_PER_TICK)),
                profile.regenerationDelay());
        notifyChange(player, before, session.pool.stamina(), profile.maximum(), action);
        if (settings.debugLogging() && allowDrainLine(session, player.world.getTotalWorldTime())) {
            LOGGER.info("JASS drained player={} action={} amount={} internal={}",
                    player.getName(), action, format(drained), format(session.pool.stamina()));
        }
        if (!session.pool.canStart()) {
            deny(player, session, action, DenialRecord.INSUFFICIENT_STAMINA);
            return false;
        }
        return true;
    }

    /**
     * Records a denial for an attempt the client already blocked, but only if the server agrees
     * the player cannot start an action now. Never charges anything.
     */
    public void reportRefusedAttempt(EntityPlayer player, ResourceLocation action) {
        if (!costsSuspended(player)) {
            refuseIfCannotStart(player, sessions.session(player), action);
        }
    }

    /** The start rule: records a denial and returns true when the player cannot start an action. */
    private boolean refuseIfCannotStart(EntityPlayer player, StaminaSession session, ResourceLocation action) {
        if (session.pool.canStart()) {
            return false;
        }
        deny(player, session, action, DenialRecord.INSUFFICIENT_STAMINA);
        return true;
    }

    /** The player's current state, or {@code null} for players without a session. */
    public StaminaReadout read(EntityPlayer player) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return null;
        }
        return new StaminaReadout(session.pool, isExempt(player), gated(player), profiles.profile(player),
                session.lastDenial, session.denialCount, session.guardBreakTicks);
    }

    /** Sets internal Stamina, clamped to the debt floor and maximum, and restarts the delay (operator and test use). */
    public void set(EntityPlayer player, double value) {
        set(player, value, JassActions.COMMAND);
    }

    /** Sets internal Stamina within the debt floor and maximum, restarting the delay; false without a session. */
    public boolean set(EntityPlayer player, double value, ResourceLocation cause) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        double before = session.pool.stamina();
        session.pool.set(value, debtFloor(profile), profile.maximum(), profile.regenerationDelay());
        notifyChange(player, before, session.pool.stamina(), profile.maximum(), cause);
        return true;
    }

    /** API: removes {@code amount} directly (no start rule, no efficiency), down to the debt floor. */
    public boolean spendDirect(EntityPlayer player, double amount, ResourceLocation cause) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        double before = session.pool.stamina();
        session.pool.payDiscrete(Math.max(0.0, amount), debtFloor(profile), profile.regenerationDelay());
        notifyChange(player, before, session.pool.stamina(), profile.maximum(), cause);
        return true;
    }

    /** API: adds {@code amount} up to the maximum, leaving the delay as it is. */
    public boolean restoreAmount(EntityPlayer player, double amount, ResourceLocation cause) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        double before = session.pool.stamina();
        session.pool.set(before + Math.max(0.0, amount), debtFloor(profile), profile.maximum(),
                session.pool.delayRemaining());
        notifyChange(player, before, session.pool.stamina(), profile.maximum(), cause);
        return true;
    }

    private static double debtFloor(StaminaProfile profile) {
        return StaminaPool.debtFloor(profile.maximum(), ConfigModel.server().maxDebtFraction());
    }

    /** Lets addons adjust or cancel a final cost (REQ-083); a cancelled event makes the action free. */
    private static double participate(EntityPlayer player, ResourceLocation action, double cost) {
        StaminaCostEvent event = new StaminaCostEvent(player, action, cost);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return 0.0;
        }
        return event.getCost();
    }

    /** Tells addons about a change (REQ-084); nothing is posted when the value did not change. */
    private static void notifyChange(EntityPlayer player, double before, double after, double maximum,
            ResourceLocation cause) {
        if (before != after) {
            MinecraftForge.EVENT_BUS.post(new StaminaChangeEvent(player, new StaminaPublicState(before, maximum),
                    new StaminaPublicState(after, maximum), cause));
        }
    }

    /** Fills Stamina to the player's maximum. */
    public void restore(EntityPlayer player) {
        StaminaSession session = sessions.session(player);
        if (session != null) {
            double maximum = profiles.profile(player).maximum();
            double before = session.pool.stamina();
            session.pool.restore(maximum);
            notifyChange(player, before, session.pool.stamina(), maximum, JassActions.COMMAND);
        }
    }

    /** Result of a Shield Block that vanilla decided to make. */
    public enum BlockOutcome {
        /** Refused at zero or below: the hit is not blocked (a denial is recorded). */
        REFUSED,
        BLOCKED,
        /** Blocked and paid, and the payment left Stamina at zero or below: Guard Break started. */
        BLOCKED_GUARD_BREAK
    }

    /**
     * Pays a Shield Block as a Discrete Action (which may create debt) and starts Guard Break
     * when the payment leaves Stamina at zero or below (REQ-037). Exempt players always block.
     */
    public BlockOutcome payBlock(EntityPlayer player, double baseCost) {
        if (costsSuspended(player)) {
            return BlockOutcome.BLOCKED;
        }
        if (!tryDiscrete(player, JassActions.BLOCK, baseCost, CostKind.STANDARD)) {
            return BlockOutcome.REFUSED;
        }
        StaminaSession session = sessions.session(player);
        if (session.pool.canStart()) {
            return BlockOutcome.BLOCKED;
        }
        session.guardBreakTicks = Math.max(1, (int) Math.round(ConfigModel.server().guardBreakCooldown() * TICKS_PER_SECOND));
        session.syncForced = true;
        if (ConfigModel.server().debugLogging()) {
            LOGGER.info("JASS guard_break player={} ticks={} internal={}",
                    player.getName(), session.guardBreakTicks, format(session.pool.stamina()));
        }
        return BlockOutcome.BLOCKED_GUARD_BREAK;
    }

    /** True while the player's Guard Break lasts: no blocking item may be raised or block. */
    public boolean guardBroken(EntityPlayer player) {
        StaminaSession session = sessions.session(player);
        return session != null && session.guardBreakTicks > 0;
    }

    /** Advances regeneration and the Guard Break timer for one server tick. */
    void tick(EntityPlayer player, double seconds) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return;
        }
        if (session.guardBreakTicks > 0) {
            session.guardBreakTicks--;
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

    /** All cost factors for the player: efficiency always, the weight multiplier for Movement Actions. */
    private static double factors(CostKind kind, StaminaProfile profile, ServerSettings settings) {
        double factors = CostCalculator.efficiencyFactor(settings.efficiencyScale(), profile.efficiency());
        if (kind == CostKind.MOVEMENT) {
            factors *= CostCalculator.weightMultiplier(profile.effectiveWeight(), settings.weightFactor());
        }
        return factors;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Drain lines have their own budget, one per second per player, so a held Continuous Action
     * never uses up the shared budget that spend and denial lines need.
     */
    private static boolean allowDrainLine(StaminaSession session, long tick) {
        if (tick - session.lastDrainLineTick < TICKS_PER_SECOND) {
            return false;
        }
        session.lastDrainLineTick = tick;
        return true;
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
