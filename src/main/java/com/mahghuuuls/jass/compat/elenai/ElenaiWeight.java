package com.mahghuuuls.jass.compat.elenai;

import com.elenai.elenaidodge2.ModConfig;
import com.elenai.elenaidodge2.init.EnchantmentInit;
import com.elenai.elenaidodge2.util.Utils;
import com.mahghuuuls.jass.gameplay.WeightSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Effective Weight from Elenai Dodge 2 (REQ-071): recomputed on the server from Elenai's own
 * configuration ({@code Weights Override}, {@code Half Feathers}) and formula for the worn armor,
 * never from the weight the client reports. Answers {@code NaN} (so the standalone weight is used)
 * while the integration is off or after a failure (ERR-1). Not included: Elenai's Endurance potion
 * and Constructs' Armory weights. Elenai's integer rounding matters: fractional overrides are summed
 * in Elenai's own order before flooring.
 */
public final class ElenaiWeight implements WeightSource {

    static final String ID = "elenai";

    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};

    private final BooleanSupplier enabled;
    private final DoubleSupplier conversion;
    private final Logger logger;
    private volatile boolean failed;
    /** Server thread only. */
    private String[] parsedSource;
    private List<WeightOverride> parsed = new ArrayList<>();

    public ElenaiWeight(BooleanSupplier enabled, DoubleSupplier conversion, Logger logger) {
        this.enabled = enabled;
        this.conversion = conversion;
        this.logger = logger;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public double effectiveWeight(EntityPlayer player) {
        if (failed || !enabled.getAsBoolean()) {
            return Double.NaN;
        }
        try {
            ItemStack[] worn = new ItemStack[ARMOR_SLOTS.length];
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                worn[i] = player.getItemStackFromSlot(ARMOR_SLOTS[i]);
            }
            double sum = sum(worn, overrides());
            int lightweight = Utils.getTotalEnchantmentLevel(EnchantmentInit.LIGHTWEIGHT, player);
            int weight = ElenaiWeightMath.elenaiWeight(sum, lightweight, ModConfig.common.feathers.half);
            return ElenaiWeightMath.effectiveWeight(weight, conversion.getAsDouble());
        } catch (LinkageError | RuntimeException e) {
            failed = true;
            logger.warn("Elenai Dodge 2 weight could not be computed ({}); the standalone weight is used", e.toString());
            return Double.NaN;
        }
    }

    /**
     * Elenai's order: for each override entry in config order, every worn piece it matches (by item
     * identity) adds its value; afterwards each piece no entry matched adds the armor formula.
     */
    private static double sum(ItemStack[] worn, List<WeightOverride> entries) {
        double sum = 0.0;
        boolean[] overridden = new boolean[worn.length];
        for (WeightOverride entry : entries) {
            for (int i = 0; i < worn.length; i++) {
                if (!worn[i].isEmpty() && worn[i].getItem() == entry.item) {
                    sum += entry.weight;
                    overridden[i] = true;
                }
            }
        }
        for (int i = 0; i < worn.length; i++) {
            if (!overridden[i] && worn[i].getItem() instanceof ItemArmor) {
                sum += ElenaiWeightMath.pieceFromArmorPoints(((ItemArmor) worn[i].getItem()).damageReduceAmount);
            }
        }
        return sum;
    }

    /** Parsed {@code Weights Override} entries, reparsed only when Elenai's config array is replaced. */
    private List<WeightOverride> overrides() {
        String[] current = ModConfig.common.weights.weights;
        if (current != parsedSource) {
            List<WeightOverride> entries = new ArrayList<>();
            for (String line : current) {
                String[] parts = line.split("=");
                if (parts.length < 2) {
                    continue;
                }
                Item item = Item.getByNameOrId(parts[0]);
                if (item == null) {
                    continue;
                }
                try {
                    entries.add(new WeightOverride(item, Double.parseDouble(parts[1])));
                } catch (NumberFormatException ignored) {
                    // Elenai would fail on this entry; JASS skips it.
                }
            }
            parsed = entries;
            parsedSource = current;
        }
        return parsed;
    }

    private static final class WeightOverride {
        final Item item;
        final double weight;

        WeightOverride(Item item, double weight) {
            this.item = item;
            this.weight = weight;
        }
    }
}
