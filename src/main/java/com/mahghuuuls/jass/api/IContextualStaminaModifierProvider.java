package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Contributes to the Stamina Profile from player context (an effect, a skill, a biome). The profile
 * is cached: call {@link JassApi#markModifierCacheDirty} when the answer changes.
 */
@FunctionalInterface
public interface IContextualStaminaModifierProvider {

    StaminaContribution getContribution(EntityPlayer player);
}
