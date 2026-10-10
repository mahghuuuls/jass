package com.mahghuuuls.jass.api.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * Client only: posted on the Forge event bus just before JASS adds its Stamina lines to an item
 * tooltip (REQ-058). Change {@link #getLines()} to edit what JASS adds, or cancel to add nothing
 * (for example while drawing the values another way). The lines are already translated and colored.
 */
@Cancelable
public final class StaminaTooltipEvent extends Event {

    private final ItemStack stack;
    private final ItemStaminaInfo info;
    private final List<String> lines;

    public StaminaTooltipEvent(ItemStack stack, ItemStaminaInfo info, List<String> lines) {
        this.stack = stack;
        this.info = info;
        this.lines = lines;
    }

    public ItemStack getStack() {
        return stack;
    }

    public ItemStaminaInfo getInfo() {
        return info;
    }

    /** The lines JASS is about to add, in order; mutable. */
    public List<String> getLines() {
        return lines;
    }
}
