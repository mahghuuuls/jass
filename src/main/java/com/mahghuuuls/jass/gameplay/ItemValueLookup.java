package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.api.ItemValueKind;
import com.mahghuuuls.jass.api.JassApi;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ItemKeys;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.ValueOrder;
import net.minecraft.item.ItemStack;

/**
 * The server's per-item values (REQ-086): the owner's configuration entry, else the first addon
 * provider's value, else JASS's default. The client's tooltips use the same order.
 */
public final class ItemValueLookup {

    private ItemValueLookup() {
    }

    public static double meleeCost(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.configuredMeleeCost(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.MELEE_COST, stack, s.defaultMeleeCost());
    }

    public static double bowDrawCost(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.configuredBowDrawCost(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.BOW_DRAW_COST, stack, s.defaultBowDrawCost());
    }

    public static double shieldStability(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.configuredShieldStability(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.SHIELD_STABILITY, stack, s.defaultShieldStability());
    }

    public static double shieldMeleeBlock(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.configuredShieldMeleeBlock(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.SHIELD_MELEE_BLOCK, stack, s.defaultShieldMeleeBlock());
    }

    public static double shieldProjectileBlock(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.configuredShieldProjectileBlock(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.SHIELD_PROJECTILE_BLOCK, stack, s.defaultShieldProjectileBlock());
    }

    /** Configured or provided standalone weight, or {@code NaN} (the caller then uses armor points). */
    public static double armorWeight(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.armorWeight(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.ARMOR_WEIGHT, stack, Double.NaN);
    }

    /** Configured or provided weight while held in a hand, else 0 (REV-014). */
    public static double heldWeight(ItemStack stack) {
        ServerSettings s = ConfigModel.server();
        return pick(s.configuredHeldWeight(ItemKeys.registryName(stack), ItemKeys.metadata(stack)),
                ItemValueKind.HELD_WEIGHT, stack, 0.0);
    }

    /** Configuration, then providers, then the default. */
    static double pick(double configured, ItemValueKind kind, ItemStack stack, double fallback) {
        // Providers are asked only when the configuration has no entry.
        return ValueOrder.first(configured,
                Double.isNaN(configured) ? JassApi.getProvidedItemValue(kind, stack) : Double.NaN, fallback);
    }
}
