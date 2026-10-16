package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.Tags;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.api.PreparationResult;
import com.mahghuuuls.jass.api.SettlementOutcome;
import com.mahghuuuls.jass.api.SettlementResult;
import com.mahghuuuls.jass.api.StaminaChangeEvent;
import com.mahghuuuls.jass.api.StaminaCostEvent;
import com.mahghuuuls.jass.api.StaminaPublicState;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.ClaimLedger;
import com.mahghuuuls.jass.core.CostCalculator;
import com.mahghuuuls.jass.core.RegenerationRules;
import com.mahghuuuls.jass.core.StaminaPool;
import com.mahghuuuls.jass.core.StaminaProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The only path that changes a player's Stamina. Applies, in order: game-mode exemption and
 * integration gating (both suspend costs and denials, never regeneration), the
 * start rule, the final cost, payment with the debt floor, and denial recording. Action hooks,
 * commands, and the API all come through here so these rules cannot diverge.
 *
 * <p>Every action is a {@link Preparation} (REQ-088): prepared (start rule on Available Stamina, a
 * provisional Stamina Claim before the one cost event, reconciliation after it), then settled or
 * released. Direct actions settle at once; addon preparations settle after their effect. Claims are
 * dropped at the end of the server tick that made them (ARC-014).
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
    /** Sessions with an addon claim opened this server tick, and their player; the end-of-tick sweep visits only these. */
    private final Map<StaminaSession, EntityPlayer> claimed = new IdentityHashMap<>();
    private long lastListenerFailureLog = Long.MIN_VALUE / 2;

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
        return session != null && available(session) > 0.0;
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
        return tryDiscrete(player, action, baseCost, kind, ItemStack.EMPTY, null);
    }

    /** As above, telling the cost event which item and hand made the action (REQ-087). */
    public boolean tryDiscrete(EntityPlayer player, ResourceLocation action, double baseCost, CostKind kind,
            ItemStack stack, EnumHand hand) {
        Preparation preparation = prepare(player, action, baseCost, kind, false, stack, hand, false);
        if (preparation.result == PreparationResult.REFUSED) {
            return false;
        }
        settle(preparation);
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
        return drainContinuous(player, action, costPerSecond, kind, ItemStack.EMPTY, null);
    }

    /** As above, telling the cost event which item and hand made the action (REQ-087). */
    public boolean drainContinuous(EntityPlayer player, ResourceLocation action, double costPerSecond, CostKind kind,
            ItemStack stack, EnumHand hand) {
        Preparation preparation = prepare(player, action, costPerSecond, kind, true, stack, hand, false);
        if (preparation.result == PreparationResult.REFUSED) {
            return false;
        }
        return settle(preparation).getOutcome() != SettlementOutcome.PAID_STOP;
    }

    /**
     * Prepares an action (REQ-088, REQ-089): suspended costs give FREE with no cost event; Available
     * Stamina at or below zero gives REFUSED and records a denial; otherwise a provisional claim is
     * opened before the one cost event, so anything a listener starts already sees it, and is then
     * reconciled to the final cost (a continuous tick reserves at most the Available Stamina above
     * zero). A final cost of zero gives FREE. Nothing is paid here.
     *
     * @param baseCost the discrete base cost, or the continuous cost per second
     */
    Preparation prepare(EntityPlayer player, ResourceLocation action, double baseCost, CostKind kind,
            boolean continuous, ItemStack stack, EnumHand hand, boolean viaApi) {
        StaminaSession session = sessions.session(player);
        if (costsSuspended(player)) {
            return prepared(player, action, continuous, stack, hand, viaApi, session, null, PreparationResult.FREE, 0.0);
        }
        if (refuseIfCannotStart(player, session, action)) {
            return prepared(player, action, continuous, stack, hand, viaApi, session, null, PreparationResult.REFUSED, 0.0);
        }
        StaminaProfile profile = profiles.profile(player);
        ServerSettings settings = ConfigModel.server();
        double factors = factors(kind, profile, settings);
        double cost = continuous ? CostCalculator.continuousCost(baseCost, factors, SECONDS_PER_TICK)
                : CostCalculator.finalDiscreteCost(baseCost, factors, settings.minDiscreteCost());
        ClaimLedger ledger = session.claims;
        ClaimLedger.Claim claim = ledger.register(continuous,
                continuous ? ClaimLedger.reserve(cost, available(session)) : cost);
        if (viaApi) {
            // Direct actions resolve their claim within this call (or release it below on an exception).
            claimed.put(session, player);
        }
        double finalCost;
        try {
            finalCost = participate(player, action, cost, stack, hand);
        } catch (RuntimeException | Error e) {
            ledger.resolve(claim, ClaimLedger.State.RELEASED);
            throw e;
        }
        if (!claim.isOpen()) {
            // A cost listener set Stamina during pricing, which invalidated the provisional claim:
            // judge the action again on the new Available Stamina.
            if (refuseIfCannotStart(player, session, action)) {
                return prepared(player, action, continuous, stack, hand, viaApi, session, null,
                        PreparationResult.REFUSED, 0.0);
            }
            claim = ledger.register(continuous, finalCost);
        }
        if (finalCost <= 0.0 && viaApi) {
            ledger.resolve(claim, ClaimLedger.State.RELEASED);
            return prepared(player, action, continuous, stack, hand, viaApi, session, null, PreparationResult.FREE, 0.0);
        }
        // A direct action with a final cost of 0 keeps its zero claim and settles through the paid
        // path, which changes nothing but still writes its debug line, as before.
        ledger.reconcile(claim, continuous
                ? ClaimLedger.reserve(finalCost, ledger.availableExcept(session.pool.stamina(), claim))
                : finalCost);
        if (continuous && viaApi && claim.amount() <= 0.0) {
            // Something started during pricing used the rest of the Available Stamina.
            ledger.resolve(claim, ClaimLedger.State.RELEASED);
            refuseIfCannotStart(player, session, action);
            return prepared(player, action, continuous, stack, hand, viaApi, session, null,
                    PreparationResult.REFUSED, 0.0);
        }
        return prepared(player, action, continuous, stack, hand, viaApi, session, claim, PreparationResult.ACCEPTED,
                finalCost);
    }

    private Preparation prepared(EntityPlayer player, ResourceLocation action, boolean continuous, ItemStack stack,
            EnumHand hand, boolean viaApi, StaminaSession session, ClaimLedger.Claim claim, PreparationResult result,
            double finalCost) {
        Preparation preparation = new Preparation(this, player, action, continuous, stack, hand, viaApi, session, claim,
                result, finalCost);
        if (viaApi && session != null && ConfigModel.server().debugLogging()
                && allowDebugLine(session, player.world.getTotalWorldTime())) {
            LOGGER.info("JASS prepare player={} action={} kind={} result={} cost={} available={}", player.getName(),
                    action, continuous ? "continuous" : "discrete", result,
                    format(preparation.getFinalCost()), format(available(session)));
        }
        return preparation;
    }

    /** Pays a preparation once (REQ-094); later calls return the first result. */
    SettlementResult settle(Preparation preparation) {
        if (preparation.settlement() != null) {
            return preparation.settlement();
        }
        SettlementResult result = settleOnce(preparation);
        preparation.recordSettlement(result);
        return result;
    }

    private SettlementResult settleOnce(Preparation p) {
        if (p.resolved() || p.result == PreparationResult.REFUSED) {
            return Preparation.nothingPaid(SettlementOutcome.INVALIDATED, publicState(p.player));
        }
        if (p.result == PreparationResult.FREE) {
            return Preparation.nothingPaid(SettlementOutcome.FREE, p.viaApi ? publicState(p.player) : null);
        }
        StaminaSession session = sessions.session(p.player);
        if (session != p.session) {
            if (p.session.claims.resolve(p.claim, ClaimLedger.State.INVALIDATED)) {
                logClaimEnd(p, "invalidate", "session");
            }
            return Preparation.nothingPaid(SettlementOutcome.INVALIDATED, publicState(p.player));
        }
        if (!session.claims.resolve(p.claim, ClaimLedger.State.SETTLED)) {
            return Preparation.nothingPaid(SettlementOutcome.INVALIDATED, publicState(p.player));
        }
        StaminaProfile profile = profiles.profile(p.player);
        ServerSettings settings = ConfigModel.server();
        double before = session.pool.stamina();
        if (p.continuous && p.viaApi) {
            session.pool.payReserve(p.claim.amount(), debtFloor(profile), profile.regenerationDelay());
        } else if (p.continuous) {
            session.pool.drainContinuous(p.claim.amount(), profile.regenerationDelay());
        } else {
            session.pool.payDiscrete(p.claim.amount(), StaminaPool.debtFloor(profile.maximum(),
                    settings.maxDebtFraction()), profile.regenerationDelay());
        }
        double after = session.pool.stamina();
        if (p.viaApi) {
            notifyChangeGuarded(p.player, before, after, profile.maximum(), p.action);
        } else {
            notifyChange(p.player, before, after, profile.maximum(), p.action);
        }
        // PAID_STOP (addon) follows Stamina itself (REQ-094); a direct drain stops when Available
        // Stamina is gone, which is the same as Stamina while no addon claim is open.
        boolean stop = p.continuous && (p.viaApi ? !session.pool.canStart() : available(session) <= 0.0);
        logSettlement(p, session, settings, before - after, stop);
        if (stop) {
            deny(p.player, session, p.action, DenialRecord.INSUFFICIENT_STAMINA);
        }
        return new SettlementResult(stop ? SettlementOutcome.PAID_STOP : SettlementOutcome.PAID, before - after,
                new StaminaPublicState(after, profile.maximum()));
    }

    /** Drops a preparation's claim with no charge, delay, or notification; nothing after a first resolution. */
    void release(Preparation p) {
        if (p.resolved()) {
            return;
        }
        p.markReleased();
        if (p.claim != null && p.session.claims.resolve(p.claim, ClaimLedger.State.RELEASED)) {
            logClaimEnd(p, "release", "addon");
        }
    }

    /** True while settling would still pay, or still return free. Changes nothing. */
    boolean isValid(Preparation p) {
        if (p.resolved() || p.result == PreparationResult.REFUSED) {
            return false;
        }
        if (p.result == PreparationResult.FREE) {
            return true;
        }
        return p.claim.isOpen() && sessions.session(p.player) == p.session;
    }

    /** End of a server tick: claims never outlive the tick that opened them (REQ-088). */
    void expireClaims() {
        if (claimed.isEmpty()) {
            return;
        }
        boolean debug = ConfigModel.server().debugLogging();
        for (Map.Entry<StaminaSession, EntityPlayer> entry : claimed.entrySet()) {
            StaminaSession session = entry.getKey();
            EntityPlayer player = entry.getValue();
            int expired = session.claims.resolveAll(ClaimLedger.State.EXPIRED);
            if (expired > 0 && debug && allowDebugLine(session, player.world.getTotalWorldTime())) {
                LOGGER.info("JASS expire player={} claims={} internal={}", player.getName(), expired,
                        format(session.pool.stamina()));
            }
        }
        claimed.clear();
    }

    /** Stamina minus open claims (REQ-089); every start rule and drain uses it. */
    private static double available(StaminaSession session) {
        return session.claims.available(session.pool.stamina());
    }

    private StaminaPublicState publicState(EntityPlayer player) {
        StaminaSession session = sessions.session(player);
        return session == null ? null : new StaminaPublicState(session.pool.stamina(), profiles.profile(player).maximum());
    }

    private void logSettlement(Preparation p, StaminaSession session, ServerSettings settings, double paid,
            boolean stop) {
        if (!settings.debugLogging()) {
            return;
        }
        long tick = p.player.world.getTotalWorldTime();
        if (p.viaApi) {
            if (allowDebugLine(session, tick)) {
                LOGGER.info("JASS settle player={} action={} outcome={} paid={} internal={}", p.player.getName(),
                        p.action, stop ? SettlementOutcome.PAID_STOP : SettlementOutcome.PAID, format(paid),
                        format(session.pool.stamina()));
            }
        } else if (p.continuous) {
            if (allowDrainLine(session, tick)) {
                LOGGER.info("JASS drained player={} action={} amount={} internal={}",
                        p.player.getName(), p.action, format(paid), format(session.pool.stamina()));
            }
        } else if (allowDebugLine(session, tick)) {
            LOGGER.info("JASS spent player={} action={} cost={} internal={}",
                    p.player.getName(), p.action, format(p.claim.amount()), format(session.pool.stamina()));
        }
    }

    private void logClaimEnd(Preparation p, String what, String cause) {
        if (ConfigModel.server().debugLogging() && allowDebugLine(p.session, p.player.world.getTotalWorldTime())) {
            LOGGER.info("JASS {} player={} action={} cause={} internal={}", what, p.player.getName(), p.action, cause,
                    format(p.session.pool.stamina()));
        }
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
        if (available(session) > 0.0) {
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
        int invalidated = session.claims.resolveAll(ClaimLedger.State.INVALIDATED);
        if (invalidated > 0 && ConfigModel.server().debugLogging()
                && allowDebugLine(session, player.world.getTotalWorldTime())) {
            LOGGER.info("JASS invalidate player={} claims={} cause=set internal={}", player.getName(), invalidated,
                    format(session.pool.stamina()));
        }
        double before = session.pool.stamina();
        session.pool.set(value, debtFloor(profile), profile.maximum(), profile.regenerationDelay());
        notifyChange(player, before, session.pool.stamina(), profile.maximum(), cause);
        return true;
    }

    /**
     * API: removes {@code amount} directly (no start rule, no efficiency), down to the debt floor,
     * but never into Stamina an open continuous claim reserves (REQ-089).
     */
    public boolean spendDirect(EntityPlayer player, double amount, ResourceLocation cause) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return false;
        }
        StaminaProfile profile = profiles.profile(player);
        double before = session.pool.stamina();
        double spend = session.claims.spendable(amount, before);
        if (spend < amount && ConfigModel.server().debugLogging()
                && allowDebugLine(session, player.world.getTotalWorldTime())) {
            LOGGER.info("JASS spend-floor player={} cause={} requested={} spent={} reserved={}", player.getName(), cause,
                    format(amount), format(spend), format(session.claims.reserved()));
        }
        session.pool.payDiscrete(spend, debtFloor(profile), profile.regenerationDelay());
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
    private static double participate(EntityPlayer player, ResourceLocation action, double cost, ItemStack stack,
            EnumHand hand) {
        StaminaCostEvent event = new StaminaCostEvent(player, action, cost, stack, hand);
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

    /** As {@link #notifyChange}, for addon settlements: a failing listener cannot undo or fail the payment (REQ-094). */
    private void notifyChangeGuarded(EntityPlayer player, double before, double after, double maximum,
            ResourceLocation cause) {
        try {
            notifyChange(player, before, after, maximum, cause);
        } catch (RuntimeException e) {
            long now = System.currentTimeMillis();
            if (now - lastListenerFailureLog >= 60_000L) {
                lastListenerFailureLog = now;
                LOGGER.warn("A Stamina change listener failed after a settled payment; the payment stays", e);
            }
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
        return payBlock(player, baseCost, ItemStack.EMPTY, null);
    }

    /** As above, with the blocking item and hand for the cost event (REQ-087). */
    public BlockOutcome payBlock(EntityPlayer player, double baseCost, ItemStack stack, EnumHand hand) {
        if (costsSuspended(player)) {
            return BlockOutcome.BLOCKED;
        }
        if (!tryDiscrete(player, JassActions.BLOCK, baseCost, CostKind.STANDARD, stack, hand)) {
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

    /**
     * Advances regeneration and the Guard Break timer for one server tick. A raised blocking item
     * slows regeneration while costs apply (REV-015).
     */
    void tick(EntityPlayer player, double seconds) {
        StaminaSession session = sessions.session(player);
        if (session == null) {
            return;
        }
        if (session.guardBreakTicks > 0) {
            session.guardBreakTicks--;
        }
        StaminaProfile profile = profiles.profile(player);
        boolean raised = player.isHandActive() && ShieldRules.isBlockingItem(player.getActiveItemStack(), player)
                && !costsSuspended(player);
        session.pool.tick(seconds, RegenerationRules.rate(profile.regeneration(), raised,
                ConfigModel.server().shieldRaisedRegeneration()), profile.maximum());
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
