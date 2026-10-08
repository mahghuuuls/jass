package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Contributes to the Stamina Profile while a registered item is in an active slot. Called on the
 * server when the profile is rebuilt (equipment change, configuration reload, or
 * {@link JassApi#markModifierCacheDirty}); exceptions are caught and logged.
 */
@FunctionalInterface
public interface IItemStaminaModifierProvider {

    StaminaContribution getContribution(EntityPlayer player, StaminaItemContext context);
}
