package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.config.ItemModifierRules;
import com.mahghuuuls.jass.config.ItemValueRules;
import com.mahghuuuls.jass.core.ModifierSet;
import com.mahghuuuls.jass.network.ItemValuesMessage;
import com.mahghuuuls.jass.network.ItemValuesMessage.Lines;
import com.mahghuuuls.jass.network.ItemValuesMessage.Number;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The server's per-item values on the client (REV-010), parsed with the same rule classes the
 * server uses, so a tooltip shows exactly what the server charges. Immutable.
 */
final class ItemValueTables {

    private final double defaultMeleeCost;
    private final ItemValueRules meleeCosts;
    private final double defaultBowDrawCost;
    private final ItemValueRules bowDrawCosts;
    private final double blockStaminaPerDamage;
    private final double stabilityScale;
    private final double defaultStability;
    private final ItemValueRules stability;
    private final double defaultMeleeBlock;
    private final ItemValueRules meleeBlock;
    private final double defaultProjectileBlock;
    private final ItemValueRules projectileBlock;
    private final ItemValueRules armorWeights;
    private final double fallbackWeightPerArmorPoint;
    private final ItemModifierRules modifiers;
    private final boolean elenaiWeightSource;
    private final Map<String, Double> elenaiOverrides;
    private final double elenaiConversion;

    ItemValueTables(ItemValuesMessage m) {
        defaultMeleeCost = m.get(Number.DEFAULT_MELEE_COST);
        meleeCosts = ItemValueRules.parse("melee_cost_overrides", m.get(Lines.MELEE_COST_OVERRIDES), 0.0);
        defaultBowDrawCost = m.get(Number.DEFAULT_BOW_DRAW_COST);
        bowDrawCosts = ItemValueRules.parse("bow_draw_cost_overrides", m.get(Lines.BOW_DRAW_COST_OVERRIDES), 0.0);
        blockStaminaPerDamage = m.get(Number.BLOCK_STAMINA_PER_DAMAGE);
        stabilityScale = m.get(Number.STABILITY_SCALE);
        defaultStability = m.get(Number.DEFAULT_SHIELD_STABILITY);
        stability = ItemValueRules.parse("shield_stability", m.get(Lines.SHIELD_STABILITY), 0.0);
        defaultMeleeBlock = m.get(Number.DEFAULT_SHIELD_MELEE_BLOCK);
        meleeBlock = ItemValueRules.parse("shield_melee_block", m.get(Lines.SHIELD_MELEE_BLOCK), 0.0, 1.0);
        defaultProjectileBlock = m.get(Number.DEFAULT_SHIELD_PROJECTILE_BLOCK);
        projectileBlock = ItemValueRules.parse("shield_projectile_block", m.get(Lines.SHIELD_PROJECTILE_BLOCK), 0.0, 1.0);
        armorWeights = ItemValueRules.parse("armor_weight", m.get(Lines.ARMOR_WEIGHT), 0.0);
        fallbackWeightPerArmorPoint = m.get(Number.FALLBACK_WEIGHT_PER_ARMOR_POINT);
        modifiers = ItemModifierRules.parse("item_stamina_modifiers", m.get(Lines.ITEM_STAMINA_MODIFIERS));
        elenaiWeightSource = m.elenaiWeightSource();
        elenaiOverrides = parseElenaiOverrides(m.get(Lines.ELENAI_WEIGHT_OVERRIDES));
        elenaiConversion = m.get(Number.ELENAI_WEIGHT_CONVERSION);
    }

    /**
     * Elenai's {@code Weights Override} entries as the server resolved them ({@code registry=weight},
     * see {@code ElenaiWeight.previewOverrideLines}); every entry for the same item adds up, as in
     * Elenai's own loop.
     */
    static Map<String, Double> parseElenaiOverrides(String[] lines) {
        Map<String, Double> sums = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length < 2) {
                continue;
            }
            try {
                String key = new ResourceLocation(parts[0].trim()).toString();
                sums.merge(key, Double.parseDouble(parts[1].trim()), Double::sum);
            } catch (RuntimeException ignored) {
                // Elenai would fail on this entry too; skipped.
            }
        }
        return Collections.unmodifiableMap(sums);
    }

    // Configured values (NaN when the item has no entry) and the server's defaults; the resolver
    // asks addon providers between the two (REQ-086), as the server does.

    double configuredMeleeCost(String name, int metadata) {
        return meleeCosts.valueFor(name, metadata, Double.NaN);
    }

    double defaultMeleeCost() {
        return defaultMeleeCost;
    }

    double configuredBowDrawCost(String name, int metadata) {
        return bowDrawCosts.valueFor(name, metadata, Double.NaN);
    }

    double defaultBowDrawCost() {
        return defaultBowDrawCost;
    }

    double blockStaminaPerDamage() {
        return blockStaminaPerDamage;
    }

    double stabilityScale() {
        return stabilityScale;
    }

    double configuredStability(String name, int metadata) {
        return stability.valueFor(name, metadata, Double.NaN);
    }

    double defaultStability() {
        return defaultStability;
    }

    double configuredMeleeBlock(String name, int metadata) {
        return meleeBlock.valueFor(name, metadata, Double.NaN);
    }

    double defaultMeleeBlock() {
        return defaultMeleeBlock;
    }

    double configuredProjectileBlock(String name, int metadata) {
        return projectileBlock.valueFor(name, metadata, Double.NaN);
    }

    double defaultProjectileBlock() {
        return defaultProjectileBlock;
    }

    /** Configured standalone weight, or {@code NaN} when the item has no entry. */
    double armorWeight(String name, int metadata) {
        return armorWeights.valueFor(name, metadata, Double.NaN);
    }

    double fallbackWeightPerArmorPoint() {
        return fallbackWeightPerArmorPoint;
    }

    /** The configured item Stamina modifier rule, or {@code null}. */
    ModifierSet modifiers(String name, int metadata) {
        return modifiers.ruleFor(name, metadata);
    }

    boolean elenaiWeightSource() {
        return elenaiWeightSource;
    }

    /** Sum of Elenai overrides for the item, or {@code NaN} when none. */
    double elenaiOverride(String name) {
        Double value = elenaiOverrides.get(name);
        return value == null ? Double.NaN : value;
    }

    double elenaiConversion() {
        return elenaiConversion;
    }
}
