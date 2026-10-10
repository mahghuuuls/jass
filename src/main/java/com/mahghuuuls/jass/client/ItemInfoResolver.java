package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.api.client.ItemStaminaInfo;
import com.mahghuuuls.jass.config.ItemKeys;
import com.mahghuuuls.jass.core.CostCalculator;
import com.mahghuuuls.jass.core.ModifierSet;
import com.mahghuuuls.jass.core.StatField;
import com.mahghuuuls.jass.core.WeightPreview;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ISpecialArmor;

/**
 * Which Stamina values an item stack shows (REQ-058), read from the server's tables with the same
 * item tests the server uses: melee weapons have an attack damage attribute in the main hand (or a
 * melee override, since every swing is charged), bow-draw items are drawn like a bow right now (the
 * server drains only those), blocking items use the block action or are shields, and armor is worn in
 * an armor slot.
 */
final class ItemInfoResolver {

    private static final int ADDITION = 0;

    private ItemInfoResolver() {
    }

    static ItemStaminaInfo resolve(ItemStack stack, EntityPlayer player, ItemValueTables t) {
        if (t == null || stack.isEmpty()) {
            return ItemStaminaInfo.NONE;
        }
        String name = ItemKeys.registryName(stack);
        int metadata = ItemKeys.metadata(stack);
        ItemStaminaInfo.Builder b = new ItemStaminaInfo.Builder();

        if (hasAttackDamage(stack) || t.hasMeleeOverride(name, metadata)) {
            b.swingCost(t.meleeCost(name, metadata));
        }
        if (stack.getItemUseAction() == EnumAction.BOW) {
            b.drawPerSecond(t.bowDrawCost(name, metadata));
        }
        if (stack.getItemUseAction() == EnumAction.BLOCK || stack.getItem().isShield(stack, player)) {
            double stability = t.stability(name, metadata);
            b.blocking(stability, t.meleeBlock(name, metadata), t.projectileBlock(name, metadata),
                    t.blockStaminaPerDamage() * CostCalculator.stabilityFactor(t.stabilityScale(), stability));
        }
        weight(stack, player, t, name, metadata, b);
        ModifierSet rule = t.modifiers(name, metadata);
        if (rule != null) {
            for (StatField field : StatField.values()) {
                double value = rule.get(field);
                if (rule.has(field) && value != 0.0) {
                    b.modifier(field.configName(), value, beneficial(field, value));
                }
            }
        }
        return b.build();
    }

    /** True when the line helps the player: more Maximum Stamina, Regeneration, or Efficiency, or less delay. */
    static boolean beneficial(StatField field, double value) {
        switch (field) {
            case MAXIMUM_REDUCTION:
            case REGENERATION_REDUCTION:
            case FLAT_DELAY:
            case DELAY_INCREASE:
                return value < 0.0;
            default:
                return value > 0.0;
        }
    }

    private static void weight(ItemStack stack, EntityPlayer player, ItemValueTables t, String name, int metadata,
            ItemStaminaInfo.Builder b) {
        EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
        if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) {
            return;
        }
        boolean isArmor = stack.getItem() instanceof ItemArmor;
        if (t.elenaiWeightSource()) {
            double override = t.elenaiOverride(name);
            if (isArmor || !Double.isNaN(override)) {
                int points = isArmor ? ((ItemArmor) stack.getItem()).damageReduceAmount : 0;
                b.weight(WeightPreview.elenaiPiece(override, isArmor, points, t.elenaiConversion()), true);
            }
            return;
        }
        double configured = t.armorWeight(name, metadata);
        double points = armorPoints(stack, player, slot);
        if (isArmor || !Double.isNaN(configured) || points > 0.0) {
            b.weight(WeightPreview.standalone(configured, points, t.fallbackWeightPerArmorPoint()), false);
        }
    }

    private static boolean hasAttackDamage(ItemStack stack) {
        return stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .containsKey(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
    }

    /** Armor points in the item's slot, as the server's standalone weight counts them. */
    private static double armorPoints(ItemStack stack, EntityPlayer player, EntityEquipmentSlot slot) {
        double points = 0.0;
        for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(SharedMonsterAttributes.ARMOR.getName())) {
            if (modifier.getOperation() == ADDITION) {
                points += modifier.getAmount();
            }
        }
        if (player != null && stack.getItem() instanceof ISpecialArmor) {
            points += ((ISpecialArmor) stack.getItem()).getArmorDisplay(player, stack, slot.getIndex());
        }
        return points;
    }
}
