package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

/** Extra active slots from an optional mod (ARC-008). The default has none. */
public interface ExtraSlotSource {

    ExtraSlotSource NONE = new ExtraSlotSource() {
        @Override
        public List<ItemStack> stacks(EntityPlayer player) {
            return Collections.emptyList();
        }

        @Override
        public String slotName() {
            return "none";
        }
    };

    /** The stacks currently in the extra slots; never {@code null}. */
    List<ItemStack> stacks(EntityPlayer player);

    /** Slot name shown in explanations, such as {@code bauble}. */
    String slotName();

    /**
     * Changes whenever the extra slots' contents change. The game posts no equipment event for these
     * slots, so the profile service polls this every 10 ticks.
     */
    default int fingerprint(EntityPlayer player) {
        int hash = 1;
        for (ItemStack stack : stacks(player)) {
            hash = 31 * hash + System.identityHashCode(stack.getItem());
            hash = 31 * hash + stack.getMetadata();
            hash = 31 * hash + stack.getCount();
            hash = 31 * hash + (stack.getTagCompound() == null ? 0 : stack.getTagCompound().hashCode());
        }
        return hash;
    }
}
