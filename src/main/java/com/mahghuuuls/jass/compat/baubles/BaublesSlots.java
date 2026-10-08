package com.mahghuuuls.jass.compat.baubles;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.mahghuuuls.jass.gameplay.ExtraSlotSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Baubles API slots as active slots (REQ-042), for Baubles and Bubbles. Uses only
 * {@code BaublesApi.getBaublesHandler}, {@code getSlots}, and {@code getStackInSlot} (DEPREF-004).
 * Loaded only when a {@code baubles} mod is present. A failure logs one warning and turns this
 * source off (ERR-1). Baubles never add weight: weight reads only armor slots.
 */
public final class BaublesSlots implements ExtraSlotSource {

    private final Logger logger;
    private volatile boolean failed;

    public BaublesSlots(Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<ItemStack> stacks(EntityPlayer player) {
        if (failed) {
            return Collections.emptyList();
        }
        try {
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if (handler == null) {
                return Collections.emptyList();
            }
            List<ItemStack> stacks = new ArrayList<>(handler.getSlots());
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack != null && !stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            return stacks;
        } catch (LinkageError | RuntimeException e) {
            failed = true;
            logger.warn("Baubles slots could not be read ({}); Baubles items no longer count for Stamina", e.toString());
            return Collections.emptyList();
        }
    }

    @Override
    public String slotName() {
        return "bauble";
    }
}
