package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ItemKeys;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.ArmorWeight;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ISpecialArmor;

import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;

/**
 * The only owner of weight-source policy (REQ-012, REQ-071): asks the preferred source for the armor
 * weight and falls back to the standalone calculation when it declines, then adds the weight of the
 * items in both hands (REV-014) whatever the source. Also the standalone source itself: the sum of
 * the four worn armor items' weights. Baubles and the rest of the inventory never add weight.
 */
final class WeightResolver implements WeightSource {

    static final String STANDALONE = "standalone";

    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
    private static final int ADDITION = 0;

    private WeightSource preferred = this;

    /** Wires an integration source; the standalone calculation stays the fallback. */
    void prefer(WeightSource source) {
        this.preferred = source;
    }

    /** The player's Effective Weight (armor plus held items), the armor source id, and the held part. */
    Resolved resolve(EntityPlayer player) {
        Resolved armor = preferred == this
                ? new Resolved(effectiveWeight(player), STANDALONE)
                : choose(preferred.effectiveWeight(player), preferred.id(), () -> effectiveWeight(player));
        return armor.withHeld(heldWeight(player));
    }

    private static double heldWeight(EntityPlayer player) {
        return heldWeight(player.getHeldItemMainhand(), player.getHeldItemOffhand(), ItemValueLookup::heldWeight);
    }

    /** Sum of the held weights of the two hand items; empty hands add 0 and the stack size does not matter. */
    static double heldWeight(ItemStack main, ItemStack off, ToDoubleFunction<ItemStack> lookup) {
        double total = 0.0;
        for (ItemStack stack : new ItemStack[]{main, off}) {
            if (!stack.isEmpty()) {
                double weight = lookup.applyAsDouble(stack);
                total += Double.isNaN(weight) ? 0.0 : Math.max(0.0, weight);
            }
        }
        return total;
    }

    /** The preferred source's weight when it is usable (a number, not negative), else the standalone weight. */
    static Resolved choose(double preferredWeight, String preferredId, DoubleSupplier standalone) {
        if (!Double.isNaN(preferredWeight) && preferredWeight >= 0.0) {
            return new Resolved(preferredWeight, preferredId);
        }
        return new Resolved(standalone.getAsDouble(), STANDALONE);
    }

    @Override
    public String id() {
        return STANDALONE;
    }

    @Override
    public double effectiveWeight(EntityPlayer player) {
        ServerSettings settings = ConfigModel.server();
        double total = 0.0;
        for (EntityEquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            double configured = ItemValueLookup.armorWeight(stack);
            total += ArmorWeight.itemWeight(configured, armorPoints(player, stack, slot),
                    settings.fallbackWeightPerArmorPoint());
        }
        return total;
    }

    /**
     * Armor points the item adds in that slot, as the armor bar counts them (Forge
     * {@code getTotalArmorValue}): flat armor modifiers plus the display value of special armor.
     */
    private static double armorPoints(EntityPlayer player, ItemStack stack, EntityEquipmentSlot slot) {
        double points = 0.0;
        for (AttributeModifier modifier : stack.getAttributeModifiers(slot)
                .get(SharedMonsterAttributes.ARMOR.getName())) {
            if (modifier.getOperation() == ADDITION) {
                points += modifier.getAmount();
            }
        }
        if (stack.getItem() instanceof ISpecialArmor) {
            points += ((ISpecialArmor) stack.getItem()).getArmorDisplay(player, stack, slot.getIndex());
        }
        return points;
    }

    static final class Resolved {
        /** Effective Weight: armor weight plus {@link #held}. */
        final double weight;
        /** The armor weight source id. */
        final String source;
        final double held;

        Resolved(double weight, String source) {
            this(weight, source, 0.0);
        }

        private Resolved(double weight, String source, double held) {
            this.weight = weight;
            this.source = source;
            this.held = held;
        }

        Resolved withHeld(double heldWeight) {
            return new Resolved(weight + heldWeight, source, held + heldWeight);
        }
    }
}
