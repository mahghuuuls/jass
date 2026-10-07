package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.StaminaProfile;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolves a player's Stamina Profile. Every stat read goes through here, so equipment and
 * addon modifiers can be added later without touching callers. A profile is cached per player
 * entity and rebuilt only after an equipment change or a configuration reload, so per-tick
 * readers (sprint, bow) never rescan equipment (ARC-007).
 */
public final class ProfileService {

    private final WeightResolver weights = new WeightResolver();
    /** Server thread only; entries go away with the player entity (respawn makes a new one). */
    private final Map<EntityPlayer, Cached> cache = new WeakHashMap<>();

    StaminaProfile profile(EntityPlayer player) {
        ServerSettings settings = ConfigModel.server();
        Cached cached = cache.get(player);
        if (cached == null || cached.revision != settings.revision()) {
            cached = new Cached(build(player, settings), settings.revision());
            cache.put(player, cached);
        }
        return cached.profile;
    }

    /** Called when the player's equipment changes; the next read rebuilds the profile. */
    void invalidate(EntityPlayer player) {
        cache.remove(player);
    }

    private StaminaProfile build(EntityPlayer player, ServerSettings settings) {
        WeightResolver.Resolved weight = weights.resolve(player);
        return new StaminaProfile(
                settings.maxStamina(),
                settings.staminaRegeneration(),
                settings.regenerationDelay(),
                0.0,
                weight.weight,
                weight.source);
    }

    private static final class Cached {
        final StaminaProfile profile;
        final int revision;

        Cached(StaminaProfile profile, int revision) {
            this.profile = profile;
            this.revision = revision;
        }
    }
}
