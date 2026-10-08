package com.mahghuuuls.jass.gameplay.hooks;

import com.mahghuuuls.jass.Tags;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.CostCalculator;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.ItemKeys;
import com.mahghuuuls.jass.gameplay.ShieldRules;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Receives vanilla's "this hit is blocked" decision from the shield Mixin. Only Shield Blocks of
 * a player against a direct entity attack or a projectile are Stamina actions: they are refused
 * at zero or below (the hit is not blocked) and otherwise pay the block cost through the gate,
 * which may create debt. Other blockable damage keeps vanilla's full block and costs nothing.
 *
 * <p>A block whose item stops less than the whole hit leaves a pending record; vanilla then
 * calls the hurt event with 0 damage for that same source in the same tick, and this hook raises
 * it to the leaked amount (before armor). Inside vanilla's hurt-resistance window vanilla skips
 * the hurt event, so nothing leaks there.
 *
 * <p>Guard Break is decided by the gate when a paid block leaves Stamina at zero or below. The
 * shield is lowered and given its cooldown on the player's next tick, after vanilla has finished
 * the block (so durability loss stays vanilla's). While Guard Break lasts no blocking item can be
 * raised or block.
 */
public final class ShieldHook {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    private static ShieldHook instance;

    private final ActionGate gate;
    /** Server thread only; at most one pending block per entity, valid for its own tick. */
    private final Map<EntityLivingBase, PendingBlock> pending = new WeakHashMap<>();
    /** Server thread only; players whose blocking item must be lowered and cooled on their next tick. */
    private final Map<EntityPlayer, Item> breakPending = new WeakHashMap<>();

    private ShieldHook(ActionGate gate) {
        this.gate = gate;
    }

    /** Creates the hook the Mixin's calls go to; the caller registers it on the event bus. */
    public static ShieldHook install(ActionGate gate) {
        instance = new ShieldHook(gate);
        return instance;
    }

    /**
     * Called by the shield Mixin only after vanilla decided to block.
     *
     * @return {@code true} to let the block happen, {@code false} to veto it (full damage)
     */
    public static boolean allowBlock(EntityLivingBase entity, DamageSource source, float amount) {
        return instance == null || instance.decide(entity, source, amount);
    }

    private boolean decide(EntityLivingBase entity, DamageSource source, float amount) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) {
            return true;
        }
        EntityPlayer player = (EntityPlayer) entity;
        boolean debug = ConfigModel.server().debugLogging();
        if (gate.guardBroken(player)) {
            // No blocking at all during Guard Break, whatever the damage.
            if (debug) {
                LOGGER.info("JASS block player={} outcome=vetoed_guard_break amount={}", player.getName(), format(amount));
            }
            return false;
        }
        ShieldRules.BlockKind kind = ShieldRules.classify(source);
        if (kind == ShieldRules.BlockKind.OTHER) {
            if (debug) {
                LOGGER.info("JASS block player={} kind=other outcome=free amount={} source={}",
                        player.getName(), format(amount), source.getDamageType());
            }
            return true;
        }
        ItemStack shield = player.getActiveItemStack();
        String name = ItemKeys.registryName(shield);
        int metadata = ItemKeys.metadata(shield);
        ServerSettings settings = ConfigModel.server();
        double stability = settings.shieldStability(name, metadata);
        double baseCost = CostCalculator.blockBaseCost(amount, settings.blockStaminaPerDamage(),
                CostCalculator.stabilityFactor(settings.stabilityScale(), stability));
        ActionGate.BlockOutcome outcome = gate.payBlock(player, baseCost);
        double fraction = kind == ShieldRules.BlockKind.MELEE
                ? settings.shieldMeleeBlock(name, metadata)
                : settings.shieldProjectileBlock(name, metadata);
        double leaked = ShieldRules.leakedDamage(amount, fraction);
        if (debug) {
            LOGGER.info("JASS block player={} kind={} outcome={} amount={} item={} stability={} baseCost={} fraction={} leaked={}",
                    player.getName(), kind.name().toLowerCase(Locale.ROOT), outcome.name().toLowerCase(Locale.ROOT),
                    format(amount), name, format(stability), format(baseCost), format(fraction), format(leaked));
        }
        if (outcome == ActionGate.BlockOutcome.REFUSED) {
            return false;
        }
        if (outcome == ActionGate.BlockOutcome.BLOCKED_GUARD_BREAK) {
            breakPending.put(player, shield.getItem());
        }
        if (leaked > 0.0) {
            pending.put(player, new PendingBlock(source, (float) leaked, player.world.getTotalWorldTime()));
        } else {
            pending.remove(player);
        }
        return true;
    }

    /**
     * Puts the item that broke on cooldown (unless something already did, such as an axe), lowers
     * it if it is still raised, and plays the shield-break sound once, only when JASS changed
     * something itself.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.START || player.world.isRemote) {
            return;
        }
        Item broken = breakPending.remove(player);
        if (broken == null) {
            return;
        }
        boolean changed = false;
        if (!player.getCooldownTracker().hasCooldown(broken)) {
            player.getCooldownTracker().setCooldown(broken, Math.max(1,
                    (int) Math.round(ConfigModel.server().guardBreakCooldown() * 20.0)));
            changed = true;
        }
        if (ShieldRules.isBlockingItem(player.getActiveItemStack(), player)) {
            player.resetActiveHand();
            changed = true;
        }
        if (changed) {
            // Entity state 30 is vanilla's shield-break sound, as when an axe disables a shield.
            player.world.setEntityState(player, (byte) 30);
        }
    }

    /** No blocking item can be raised while Guard Break lasts. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity instanceof EntityPlayerMP && ShieldRules.isBlockingItem(event.getItem(), entity)
                && gate.guardBroken((EntityPlayer) entity)) {
            event.setCanceled(true);
        }
    }

    /** A respawned player keeps its entity id; drop anything pending from before death. */
    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        breakPending.remove(event.player);
        pending.remove(event.player);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Raises vanilla's 0-damage hurt call after a partial block to the leaked amount. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) {
            return;
        }
        PendingBlock block = pending.remove(entity);
        if (block != null && block.source == event.getSource() && block.tick == entity.world.getTotalWorldTime()
                && event.getAmount() == 0.0F) {
            event.setAmount(block.leaked);
        }
    }

    private static final class PendingBlock {
        final DamageSource source;
        final float leaked;
        final long tick;

        PendingBlock(DamageSource source, float leaked, long tick) {
            this.source = source;
            this.leaked = leaked;
            this.tick = tick;
        }
    }
}
