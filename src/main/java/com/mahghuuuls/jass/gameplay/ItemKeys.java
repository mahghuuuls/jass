package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ItemValueRules;
import net.minecraft.item.ItemStack;

/**
 * How an item stack is named when looking it up in a per-item configuration list. The only
 * owner of the empty-hand name and of what metadata means for a lookup.
 */
public final class ItemKeys {

    /** The name an empty hand is looked up under. */
    public static final String EMPTY_HAND = "minecraft:air";

    private ItemKeys() {
    }

    public static String registryName(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return EMPTY_HAND;
        }
        return stack.getItem().getRegistryName().toString();
    }

    /**
     * The metadata to match. For an item with durability the metadata is its damage, which
     * changes with wear, so such items match only their wildcard entry.
     */
    public static int metadata(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return stack.getItem().isDamageable() ? ItemValueRules.ANY_METADATA : stack.getMetadata();
    }
}
