package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Entry point of the JASS addon API (ARC-009). Providers register during mod loading; registration
 * closes when loading completes, and a later registration throws.
 */
public final class JassApi {

    public static final String API_VERSION = "1.0";

    private static IStaminaService service;
    private static Consumer<EntityPlayer> cacheInvalidator = player -> { };
    private static volatile boolean frozen;
    private static final Set<ResourceLocation> ids = new HashSet<>();
    private static final Map<Item, List<RegisteredProvider<IItemStaminaModifierProvider>>> itemProviders = new HashMap<>();
    private static final List<RegisteredProvider<IContextualStaminaModifierProvider>> contextualProviders = new ArrayList<>();

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
