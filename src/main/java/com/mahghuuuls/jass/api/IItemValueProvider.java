package com.mahghuuuls.jass.api;

import net.minecraft.item.ItemStack;

/**
 * Default Stamina values for items that JASS's configuration does not list (REQ-086), for example
 * per weapon type of another mod. The owner's configuration always wins; a provider's value wins
 * over JASS's built-in default. Return {@code Double.NaN} (the default) for "no value".
 *
 * <p>Called on both sides: on the server for costs, and on the client for tooltips; in single player
 * the server and client threads may call at the same time, so answers must be thread-safe. Answers
 * must be cheap, must not change world state or the stack, and must be the same on both sides (an
 * addon with its own configuration syncs it itself; a client without the addon shows JASS defaults).
 * Values must be finite and not negative; block shares are 0 to 1. A provider that throws a
 * {@code LinkageError} is switched off for the session.
 */
public interface IItemValueProvider {

    /** Stamina per melee swing with this item in hand. */
    default double meleeCost(ItemStack stack) {
        return Double.NaN;
    }

    /** Stamina per second while this bow-draw item is drawn; 0 means free and never interrupted. */
    default double bowDrawCost(ItemStack stack) {
        return Double.NaN;
    }

    /** Stability of this blocking item. */
    default double shieldStability(ItemStack stack) {
        return Double.NaN;
    }

    /** Fraction of a blocked melee hit this blocking item stops, 0 to 1. */
    default double shieldMeleeBlock(ItemStack stack) {
        return Double.NaN;
    }

    /** Fraction of a blocked projectile hit this blocking item stops, 0 to 1. */
    default double shieldProjectileBlock(ItemStack stack) {
        return Double.NaN;
    }

    /** Standalone weight of this item worn in an armor slot (not used while Elenai Dodge 2 gives the weight). */
    default double armorWeight(ItemStack stack) {
        return Double.NaN;
    }
}
