package com.mahghuuuls.jass.api;

import net.minecraft.item.ItemStack;

/** Where an item that a provider is asked about sits. */
public final class StaminaItemContext {

    private final ItemStack stack;
    private final String slot;

    public StaminaItemContext(ItemStack stack, String slot) {
        this.stack = stack;
        this.slot = slot;
    }

    public ItemStack getStack() {
        return stack;
    }

    /** {@code head}, {@code chest}, {@code legs}, {@code feet}, {@code mainhand}, {@code offhand}, or {@code bauble}. */
    public String getSlot() {
        return slot;
    }
}
