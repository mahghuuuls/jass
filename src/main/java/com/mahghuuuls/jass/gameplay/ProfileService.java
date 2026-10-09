package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.Tags;
import com.mahghuuuls.jass.api.IContextualStaminaModifierProvider;
import com.mahghuuuls.jass.api.IItemStaminaModifierProvider;
import com.mahghuuuls.jass.api.JassApi;
import com.mahghuuuls.jass.api.RegisteredProvider;
import com.mahghuuuls.jass.api.StaminaContribution;
import com.mahghuuuls.jass.api.StaminaItemContext;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.ModifierSet;
import com.mahghuuuls.jass.core.ProfileAggregator;
import com.mahghuuuls.jass.core.StaminaProfile;
import com.mahghuuuls.jass.core.StatField;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolves a player's Stamina Profile. Every stat read goes through here, so equipment and
 * addon modifiers can be added later without touching callers. A profile is cached per player
 * entity and rebuilt only after an equipment change or a configuration reload, so per-tick
 * readers (sprint, bow) never rescan equipment (ARC-007).
 */
public final class ProfileService {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    private static final long PROVIDER_ERROR_LOG_INTERVAL_MILLIS = 60_000L;
    private static final long EXTRA_SLOT_POLL_TICKS = 10L;

    /** Provider id -> last time a failure was logged; at most one line per provider per minute (ERR-2). */
    private final Map<String, Long> lastProviderError = new HashMap<>();

    private final WeightResolver weights = new WeightResolver();
    private final ActiveSlotCollector slots = new ActiveSlotCollector();
    /**
     * Server thread only. Entries go away with the player entity; a respawned player keeps its entity
     * id, so the lifecycle invalidates the entry explicitly on respawn.
     */
    private final Map<EntityPlayer, Cached> cache = new WeakHashMap<>();

    StaminaProfile profile(EntityPlayer player) {
        ServerSettings settings = ConfigModel.server();
        Cached cached = cache.get(player);
        if (cached == null || cached.revision != settings.revision()) {
            cached = new Cached(build(player, settings), settings.revision(), slots.extraFingerprint(player));
            cache.put(player, cached);
        }
        return cached.profile;
    }

    /** Wires an integration's weight source, such as Elenai; the standalone weight stays the fallback. */
    void preferWeightSource(WeightSource source) {
        weights.prefer(source);
    }

    /** Wires an integration's extra active slots, such as Baubles. */
    void addExtraSlots(ExtraSlotSource source) {
        slots.addSource(source);
    }

    /**
     * Every 10 ticks, rebuilds the profile on the next read when the extra slots changed (the game
     * posts no equipment event for them). Cheap: only the extra slots are fingerprinted.
     */
    void pollExtraSlots(EntityPlayer player, long worldTick) {
        if (worldTick % EXTRA_SLOT_POLL_TICKS != 0) {
            return;
        }
        Cached cached = cache.get(player);
        if (cached != null && cached.extraFingerprint != slots.extraFingerprint(player)) {
            cache.remove(player);
        }
    }

    /** Called when the player's equipment changes; the next read rebuilds the profile. */
    void invalidate(EntityPlayer player) {
        cache.remove(player);
    }

    private StaminaProfile build(EntityPlayer player, ServerSettings settings) {
        List<ModifierSet> contributions = contributions(player, settings);
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(settings.maxStamina(),
                settings.staminaRegeneration(), settings.regenerationDelay(), contributions);
        List<String> explanations = new ArrayList<>();
        for (ModifierSet set : contributions) {
            explanations.add(set.source() + " " + set.describeValues());
        }
        WeightResolver.Resolved weight = weights.resolve(player);
        return new StaminaProfile(stats.maximum, stats.regeneration, stats.regenerationDelay, stats.efficiency,
                weight.weight, weight.source, explanations);
    }

    /** Item rules and item providers for every stack in an active slot, then contextual providers. */
    private List<ModifierSet> contributions(EntityPlayer player, ServerSettings settings) {
        List<ModifierSet> contributions = new ArrayList<>();
        for (ActiveSlotCollector.ActiveStack active : slots.collect(player)) {
            ModifierSet rule = settings.itemModifiers().ruleFor(ItemKeys.registryName(active.stack),
                    ItemKeys.metadata(active.stack));
            if (rule != null) {
                contributions.add(rule.withSource("item " + rule.source() + " (" + active.slot + ")"));
            }
            for (RegisteredProvider<IItemStaminaModifierProvider> entry : JassApi.getItemProviders(active.stack.getItem())) {
                if (settings.providerDisabled(entry.getId().toString())) {
                    continue;
                }
                StaminaContribution contribution;
                try {
                    contribution = entry.getProvider().getContribution(player,
                            new StaminaItemContext(active.stack, active.slot));
                } catch (RuntimeException | LinkageError e) {
                    providerFailed(entry.getId().toString(), e);
                    continue;
                }
                add(contributions, contribution, "provider " + entry.getId() + " (" + active.slot + ")");
            }
        }
        for (RegisteredProvider<IContextualStaminaModifierProvider> entry : JassApi.getContextualProviders()) {
            if (settings.providerDisabled(entry.getId().toString())) {
                continue;
            }
            StaminaContribution contribution;
            try {
                contribution = entry.getProvider().getContribution(player);
            } catch (RuntimeException | LinkageError e) {
                providerFailed(entry.getId().toString(), e);
                continue;
            }
            add(contributions, contribution, "provider " + entry.getId());
        }
        return contributions;
    }

    private static void add(List<ModifierSet> contributions, StaminaContribution c, String source) {
        if (c == null || c.isEmpty()) {
            return;
        }
        Map<StatField, Double> values = new EnumMap<>(StatField.class);
        put(values, StatField.FLAT_MAXIMUM, c.getFlatMaximum());
        put(values, StatField.MAXIMUM_INCREASE, c.getMaximumIncrease());
        put(values, StatField.MAXIMUM_REDUCTION, c.getMaximumReduction());
        put(values, StatField.FLAT_REGENERATION, c.getFlatRegeneration());
        put(values, StatField.REGENERATION_INCREASE, c.getRegenerationIncrease());
        put(values, StatField.REGENERATION_REDUCTION, c.getRegenerationReduction());
        put(values, StatField.FLAT_DELAY, c.getFlatDelay());
        put(values, StatField.DELAY_INCREASE, c.getDelayIncrease());
        put(values, StatField.DELAY_REDUCTION, c.getDelayReduction());
        put(values, StatField.EFFICIENCY, c.getEfficiency());
        contributions.add(new ModifierSet(values, source));
    }

    private static void put(Map<StatField, Double> values, StatField field, double value) {
        if (value != 0.0) {
            values.put(field, value);
        }
    }

    /** A failing provider is skipped for this calculation and logged at most once a minute (ERR-2). */
    private void providerFailed(String id, Throwable e) {
        long now = System.currentTimeMillis();
        Long last = lastProviderError.get(id);
        if (last == null || now - last >= PROVIDER_ERROR_LOG_INTERVAL_MILLIS) {
            lastProviderError.put(id, now);
            LOGGER.warn("Stamina modifier provider {} failed and was skipped: {}", id, e.toString());
        }
    }

    private static final class Cached {
        final StaminaProfile profile;
        final int revision;
        final int extraFingerprint;

        Cached(StaminaProfile profile, int revision, int extraFingerprint) {
            this.profile = profile;
            this.revision = revision;
            this.extraFingerprint = extraFingerprint;
        }
    }
}
