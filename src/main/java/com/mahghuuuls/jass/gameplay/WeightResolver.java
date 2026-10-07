package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.ArmorWeight;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ISpecialArmor;

/**
 * The only owner of weight-source policy (REQ-012, REQ-071): asks the preferred source and falls
 * back to the standalone calculation when it declines. Also the standalone source itself: the sum
 * of the four worn armor items' weights. Held items, shields, and Baubles never add weight.
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

    /** The player's Effective Weight and the id of the source that gave it. */
    Resolved resolve(EntityPlayer player) {
        if (preferred != this) {
            double weight = preferred.effectiveWeight(player);
            if (!Double.isNaN(weight) && weight >= 0.0) {
                return new Resolved(weight, preferred.id());
            }
        }
        return new Resolved(effectiveWeight(player), STANDALONE);
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
            double configured = settings.armorWeight(ItemKeys.registryName(stack), ItemKeys.metadata(stack));
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
        final double weight;
        final String source;

        Resolved(double weight, String source) {
            this.weight = weight;
            this.source = source;
        }
    }
}
