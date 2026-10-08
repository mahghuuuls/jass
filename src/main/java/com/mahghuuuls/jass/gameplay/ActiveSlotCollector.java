package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the stacks in a player's active slots (REQ-040): the four armor slots, main hand, off hand,
 * and the slots of an extra source such as Baubles when one is wired.
 */
final class ActiveSlotCollector {

    /** One non-empty stack and the slot name shown in explanations. */
    static final class ActiveStack {
        final ItemStack stack;
        final String slot;

        ActiveStack(ItemStack stack, String slot) {
            this.stack = stack;
            this.slot = slot;
        }
    }

    private static final EntityEquipmentSlot[] SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET,
            EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND};

    private ExtraSlotSource extra = ExtraSlotSource.NONE;

    /** Wires an integration's extra slots, such as Baubles. */
    void addSource(ExtraSlotSource source) {
        this.extra = source;
    }

    /** Fingerprint of the extra slots, for change polling. */
    int extraFingerprint(EntityPlayer player) {
        return extra == ExtraSlotSource.NONE ? 0 : extra.fingerprint(player);
    }

    List<ActiveStack> collect(EntityPlayer player) {
        List<ActiveStack> stacks = new ArrayList<>();
        for (EntityEquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(new ActiveStack(stack, slot.getName()));
            }
        }
        for (ItemStack stack : extra.stacks(player)) {
            if (!stack.isEmpty()) {
                stacks.add(new ActiveStack(stack, extra.slotName()));
            }
        }
        return stacks;
    }
}
