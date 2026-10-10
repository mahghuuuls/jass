package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Entry point of the JASS addon API (ARC-009). Providers register during mod loading; registration
 * closes when loading completes, and a later registration throws.
 */
public final class JassApi {

    public static final String API_VERSION = "1.0";

    private static final Logger LOGGER = LogManager.getLogger("JASS API");
    private static final long PROVIDER_ERROR_LOG_INTERVAL_MILLIS = 60_000L;

    private static IStaminaService service;
    private static Consumer<EntityPlayer> cacheInvalidator = player -> { };
    private static volatile boolean frozen;
    private static final Set<ResourceLocation> ids = new HashSet<>();
    private static final Map<Item, List<RegisteredProvider<IItemStaminaModifierProvider>>> itemProviders = new HashMap<>();
    private static final List<RegisteredProvider<IContextualStaminaModifierProvider>> contextualProviders = new ArrayList<>();
    private static final List<RegisteredProvider<IItemValueProvider>> itemValueProviders = new CopyOnWriteArrayList<>();
    /** Provider id -> last time a failure was logged; at most one line per provider per minute (ERR-2). */
    private static final Map<ResourceLocation, Long> itemValueFailureLog = new ConcurrentHashMap<>();
    /** Providers switched off after a linkage failure (a missing class fails the same way every call). */
    private static final Set<ResourceLocation> disabledItemValueProviders = ConcurrentHashMap.newKeySet();

    private JassApi() {
    }

    /** The server service; available from JASS pre-initialization on. */
    public static IStaminaService getStaminaService() {
        if (service == null) {
            throw new IllegalStateException("JASS has not initialized its Stamina service yet");
        }
        return service;
    }

    /** Registers a provider called while {@code item} is in an active slot. */
    public static synchronized void registerItemProvider(ResourceLocation id, Item item,
            IItemStaminaModifierProvider provider) {
        checkOpen(id);
        if (item == null || provider == null) {
            throw new IllegalArgumentException("item and provider are required");
        }
        ids.add(id);
        itemProviders.computeIfAbsent(item, i -> new ArrayList<>()).add(new RegisteredProvider<>(id, provider));
    }

    /** Registers a provider called for every player whenever the profile is rebuilt. */
    public static synchronized void registerContextualProvider(ResourceLocation id,
            IContextualStaminaModifierProvider provider) {
        checkOpen(id);
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }
        ids.add(id);
        contextualProviders.add(new RegisteredProvider<>(id, provider));
    }

    /**
     * Registers default item values for items JASS's configuration does not list (REQ-086). Register
     * on both sides (common code): JASS asks on the server for costs and on the client for tooltips.
     * Providers are asked in registration order; the first usable answer wins.
     */
    public static synchronized void registerItemValueProvider(ResourceLocation id, IItemValueProvider provider) {
        checkOpen(id);
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }
        ids.add(id);
        itemValueProviders.add(new RegisteredProvider<>(id, provider));
    }

    /**
     * The first usable provider value for the stack, or {@code NaN} when no provider answers. Read
     * by JASS after its own configuration and before its built-in default. A provider that throws or
     * answers an unusable number is skipped for that call and logged at most once a minute.
     */
    public static double getProvidedItemValue(ItemValueKind kind, ItemStack stack) {
        if (stack == null || stack.isEmpty() || itemValueProviders.isEmpty()) {
            return Double.NaN;
        }
        return firstUsable(itemValueProviders, kind, stack);
    }

    /** The first usable answer in order; unusable answers and failures are skipped and logged. */
    static double firstUsable(List<RegisteredProvider<IItemValueProvider>> providers, ItemValueKind kind, ItemStack stack) {
        for (RegisteredProvider<IItemValueProvider> entry : providers) {
            if (disabledItemValueProviders.contains(entry.getId())) {
                continue;
            }
            double value;
            try {
                value = kind.ask(entry.getProvider(), stack);
            } catch (LinkageError e) {
                disabledItemValueProviders.add(entry.getId());
                LOGGER.warn("JASS item value provider {} switched off after a linkage failure: {}", entry.getId(), e.toString());
                continue;
            } catch (RuntimeException e) {
                logFailure(entry.getId(), kind + " failed: " + e);
                continue;
            }
            if (Double.isNaN(value)) {
                continue;
            }
            if (kind.valid(value)) {
                return value;
            }
            logFailure(entry.getId(), kind + " answered an unusable value " + value);
        }
        return Double.NaN;
    }

    private static void logFailure(ResourceLocation id, String problem) {
        long now = System.currentTimeMillis();
        Long last = itemValueFailureLog.get(id);
        if (last == null || now - last >= PROVIDER_ERROR_LOG_INTERVAL_MILLIS) {
            itemValueFailureLog.put(id, now);
            LOGGER.warn("JASS item value provider {} skipped: {}", id, problem);
        }
    }

    /**
     * Asks JASS to rebuild the player's profile on its next read (a contextual answer changed).
     * Server side only; calls for a client player are ignored.
     */
    public static void markModifierCacheDirty(EntityPlayer player) {
        if (player != null && !player.world.isRemote) {
            cacheInvalidator.accept(player);
        }
    }

    public static boolean isProviderRegistrationFrozen() {
        return frozen;
    }

    /** Providers registered for an item; read by JASS when it builds a profile. */
    public static List<RegisteredProvider<IItemStaminaModifierProvider>> getItemProviders(Item item) {
        List<RegisteredProvider<IItemStaminaModifierProvider>> list = itemProviders.get(item);
        return list == null ? Collections.<RegisteredProvider<IItemStaminaModifierProvider>>emptyList()
                : Collections.unmodifiableList(list);
    }

    /** Contextual providers; read by JASS when it builds a profile. */
    public static List<RegisteredProvider<IContextualStaminaModifierProvider>> getContextualProviders() {
        return Collections.unmodifiableList(contextualProviders);
    }

    /**
     * Called once by JASS itself; a second call throws. The returned handle is how JASS closes
     * registration, so no addon can close it early.
     */
    public static synchronized Control bind(IStaminaService stamina, Consumer<EntityPlayer> invalidator) {
        if (service != null) {
            throw new IllegalStateException("JASS's Stamina service is already bound");
        }
        service = stamina;
        cacheInvalidator = invalidator;
        return new Control();
    }

    /** JASS's own handle on the facade. */
    public static final class Control {

        private Control() {
        }

        /** Closes provider registration (when loading completes). */
        public void freezeRegistration() {
            frozen = true;
        }
    }

    private static void checkOpen(ResourceLocation id) {
        if (frozen) {
            throw new IllegalStateException("JASS provider registration closed when loading completed: " + id);
        }
        if (id == null) {
            throw new IllegalArgumentException("provider id is required");
        }
        if (ids.contains(id)) {
            throw new IllegalArgumentException("provider id already registered: " + id);
        }
    }
}
