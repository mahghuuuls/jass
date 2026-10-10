package com.mahghuuuls.jass.api;

import net.minecraft.item.ItemStack;

/** The per-item values an {@link IItemValueProvider} can supply (REQ-086). */
public enum ItemValueKind {
    MELEE_COST(Double.POSITIVE_INFINITY),
    BOW_DRAW_COST(Double.POSITIVE_INFINITY),
    SHIELD_STABILITY(Double.POSITIVE_INFINITY),
    SHIELD_MELEE_BLOCK(1.0),
    SHIELD_PROJECTILE_BLOCK(1.0),
    ARMOR_WEIGHT(Double.POSITIVE_INFINITY);

    private final double maximum;

    ItemValueKind(double maximum) {
        this.maximum = maximum;
    }

    /** True for a usable answer: a finite number from 0 to this kind's maximum. */
    boolean valid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0 && value <= maximum;
    }

    double ask(IItemValueProvider provider, ItemStack stack) {
        switch (this) {
            case MELEE_COST:
                return provider.meleeCost(stack);
            case BOW_DRAW_COST:
                return provider.bowDrawCost(stack);
            case SHIELD_STABILITY:
                return provider.shieldStability(stack);
            case SHIELD_MELEE_BLOCK:
                return provider.shieldMeleeBlock(stack);
            case SHIELD_PROJECTILE_BLOCK:
                return provider.shieldProjectileBlock(stack);
            default:
                return provider.armorWeight(stack);
        }
    }
}
