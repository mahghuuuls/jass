package com.mahghuuuls.jass.config;

import com.mahghuuuls.jass.Tags;
import net.minecraftforge.common.config.Config;

/**
 * Server-side gameplay settings, written to {@code config/jass/jass.cfg}. Key names match the
 * keys of the project's balance defaults table.
 */
@Config(modid = Tags.MOD_ID, name = "jass/jass", category = "stamina")
public final class JassConfig {

    private JassConfig() {
    }

    @Config.Name("max_stamina")
    @Config.Comment("Base Maximum Stamina before item modifiers.")
    @Config.RangeDouble(min = 1.0, max = 100000.0)
    public static double maxStamina = 60.0;

    @Config.Name("stamina_regeneration")
    @Config.Comment("Stamina regained per second once the regeneration delay has passed. Repays Stamina Debt first.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double staminaRegeneration = 25.0;

    @Config.Name("regeneration_delay")
    @Config.Comment("Seconds after spending Stamina before it starts to regenerate.")
    @Config.RangeDouble(min = 0.0, max = 3600.0)
    public static double regenerationDelay = 0.75;

    @Config.Name("max_debt_fraction")
    @Config.Comment({
            "How far below zero Stamina may go, as a fraction of Maximum Stamina.",
            "1.0 allows debt down to -Maximum Stamina. -1 means no limit."})
    @Config.RangeDouble(min = -1.0, max = 100.0)
    public static double maxDebtFraction = 1.0;

    @Config.Name("efficiency_scale")
    @Config.Comment("Diminishing-returns constant: costs are multiplied by scale / (scale + Stamina Efficiency).")
    @Config.RangeDouble(min = 0.001, max = 100000.0)
    public static double efficiencyScale = 100.0;

    @Config.Name("min_discrete_cost")
    @Config.Comment("Smallest cost of a one-time action whose configured cost is above zero.")
    @Config.RangeDouble(min = 0.001, max = 100000.0)
    public static double minDiscreteCost = 1.0;

    @Config.Name("creative_consumption")
    @Config.Comment("If true, Creative-mode players spend Stamina like Survival players.")
    public static boolean creativeConsumption = false;

    @Config.Name("sprint_cost")
    @Config.Comment("Stamina drained per second while sprinting on land. Swimming costs nothing.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double sprintCost = 5.0;

    @Config.Name("jump_cost_enabled")
    @Config.Comment("If true, each jump from the ground costs jump_cost (times weight and efficiency) and cannot start at zero Stamina.")
    public static boolean jumpCostEnabled = false;

    @Config.Name("jump_cost")
    @Config.Comment("Stamina cost of one jump from the ground when jump_cost_enabled is true. Swimming, climbing, and flying are not jumps.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double jumpCost = 12.0;

    @Config.Name("weight_factor")
    @Config.Comment("Movement actions (sprint, jump, Elenai dodge) cost 1 + Effective Weight x this value times more.")
    @Config.RangeDouble(min = 0.0, max = 100.0)
    public static double weightFactor = 0.02;

    @Config.Name("armor_weight")
    @Config.Comment({
            "Weight of worn armor items, one entry per line: namespace:item[@metadata or @*]=weight",
            "Worn armor without an entry weighs its armor points x fallback_weight_per_armor_point.",
            "Held items, shields, and Baubles never add weight. Metadata is ignored for items with durability."})
    public static String[] armorWeight = {
            "minecraft:leather_helmet=1",
            "minecraft:leather_chestplate=2",
            "minecraft:leather_leggings=2",
            "minecraft:leather_boots=1",
            "minecraft:golden_helmet=2",
            "minecraft:golden_chestplate=4",
            "minecraft:golden_leggings=3",
            "minecraft:golden_boots=2",
            "minecraft:chainmail_helmet=2",
            "minecraft:chainmail_chestplate=4",
            "minecraft:chainmail_leggings=3",
            "minecraft:chainmail_boots=2",
            "minecraft:iron_helmet=3",
            "minecraft:iron_chestplate=6",
            "minecraft:iron_leggings=5",
            "minecraft:iron_boots=3",
            "minecraft:diamond_helmet=4",
            "minecraft:diamond_chestplate=8",
            "minecraft:diamond_leggings=7",
            "minecraft:diamond_boots=4"
    };

    @Config.Name("fallback_weight_per_armor_point")
    @Config.Comment("Weight per armor point for worn armor that has no armor_weight entry.")
    @Config.RangeDouble(min = 0.0, max = 1000.0)
    public static double fallbackWeightPerArmorPoint = 1.0;

    @Config.Name("default_melee_cost")
    @Config.Comment("Stamina cost of one melee swing (hit or miss) with any item, or an empty hand, that has no entry in melee_cost_overrides.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double defaultMeleeCost = 12.0;

    @Config.Name("melee_cost_overrides")
    @Config.Comment({
            "Melee swing cost per item, one entry per line: namespace:item[@metadata or @*]=cost",
            "Example: minecraft:iron_sword=20   A cost of 0 makes swings with that item free.",
            "Metadata is ignored for items with durability. minecraft:air sets the empty-hand cost."})
    public static String[] meleeCostOverrides = {};

    @Config.Name("default_bow_draw_cost")
    @Config.Comment("Stamina drained per second while drawing or holding a bow-draw item (bows and anything drawn like a bow) that has no entry in bow_draw_cost_overrides.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double defaultBowDrawCost = 8.0;

    @Config.Name("bow_draw_cost_overrides")
    @Config.Comment({
            "Draw cost per second per item, one entry per line: namespace:item[@metadata or @*]=rate",
            "Example: minecraft:bow=4   A rate of 0 means drawing that item never drains Stamina and is never",
            "interrupted (it still cannot start at zero Stamina). Metadata is ignored for items with durability."})
    public static String[] bowDrawCostOverrides = {};

    @Config.Name("block_stamina_per_damage")
    @Config.Comment("Stamina a Shield Block costs per point of incoming damage (after difficulty, before armor), before Stability and efficiency.")
    @Config.RangeDouble(min = 0.0, max = 1000.0)
    public static double blockStaminaPerDamage = 1.0;

    @Config.Name("stability_scale")
    @Config.Comment("Diminishing-returns constant for shield Stability: block costs are multiplied by scale / (scale + Stability).")
    @Config.RangeDouble(min = 0.001, max = 100000.0)
    public static double stabilityScale = 100.0;

    @Config.Name("shield_stability")
    @Config.Comment({
            "Stability of blocking items, one entry per line: namespace:item[@metadata or @*]=stability",
            "Higher Stability makes blocks cheaper. Items without an entry use default_shield_stability.",
            "Metadata is ignored for items with durability."})
    public static String[] shieldStability = {
            "minecraft:shield=50"
    };

    @Config.Name("default_shield_stability")
    @Config.Comment("Stability of a blocking item that has no shield_stability entry.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double defaultShieldStability = 0.0;

    @Config.Name("shield_melee_block")
    @Config.Comment({
            "Fraction of a blocked direct melee hit that a blocking item stops, one entry per line: namespace:item[@metadata or @*]=fraction",
            "1.0 blocks everything (vanilla); 0.7 lets 30 percent of the hit through. Items without an entry use default_shield_melee_block."})
    public static String[] shieldMeleeBlock = {
            "minecraft:shield=1.0"
    };

    @Config.Name("shield_projectile_block")
    @Config.Comment({
            "Fraction of a blocked projectile hit that a blocking item stops, one entry per line: namespace:item[@metadata or @*]=fraction",
            "Items without an entry use default_shield_projectile_block."})
    public static String[] shieldProjectileBlock = {
            "minecraft:shield=1.0"
    };

    @Config.Name("default_shield_melee_block")
    @Config.Comment("Melee block fraction of a blocking item without a shield_melee_block entry (1.0 = vanilla full block).")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double defaultShieldMeleeBlock = 1.0;

    @Config.Name("default_shield_projectile_block")
    @Config.Comment("Projectile block fraction of a blocking item without a shield_projectile_block entry (1.0 = vanilla full block).")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double defaultShieldProjectileBlock = 1.0;

    @Config.Name("guard_break_cooldown")
    @Config.Comment("Seconds a player cannot raise any blocking item after a block leaves Stamina at zero or below (Guard Break).")
    @Config.RangeDouble(min = 0.0, max = 3600.0)
    public static double guardBreakCooldown = 2.0;

    @Config.Name("item_stamina_modifiers")
    @Config.Comment({
            "Item Stamina Modifiers, one rule per line: namespace:item[@metadata or @*]=field:value;field:value",
            "A rule applies while the item is worn in an armor slot, held in either hand, or worn in a bauble slot (Baubles or Bubbles).",
            "Fields: flat_maximum, maximum_increase, maximum_reduction, flat_regeneration, regeneration_increase,",
            "regeneration_reduction, flat_delay, delay_increase, delay_reduction, efficiency.",
            "Increases and reductions are fractions (0.5 = 50 percent); reductions must be from 0 to 1.",
            "Example: minecraft:golden_boots=flat_regeneration:25;delay_reduction:0.5"})
    public static String[] itemStaminaModifiers = {};

    @Config.Name("inhibited_integration")
    @Config.Comment("With Inhibited installed: if true, Stamina costs apply only while the player has the Inhibited effect. No effect without Inhibited.")
    public static boolean inhibitedIntegration = true;

    @Config.Name("disabled_stamina_providers")
    @Config.Comment("Addon Stamina modifier providers to ignore, one provider id per line (namespace:path). Empty allows all.")
    public static String[] disabledStaminaProviders = {};

    @Config.Name("rlcombat_integration")
    @Config.Comment("With RLCombat installed: if true, RLCombat off-hand attacks cost the off-hand item's melee cost. If false, every attack uses the main-hand item.")
    public static boolean rlcombatIntegration = true;

    @Config.Name("elenai_dodge_resource_integration")
    @Config.Comment("With Elenai Dodge 2 installed: if true, each dodge costs Stamina (elenai_dodge_base_cost, times weight and efficiency) instead of feathers, and is refused at zero Stamina.")
    public static boolean elenaiDodgeResourceIntegration = true;

    @Config.Name("elenai_dodge_base_cost")
    @Config.Comment("Stamina cost of one Elenai dodge before the weight multiplier and efficiency.")
    @Config.RangeDouble(min = 0.0, max = 100000.0)
    public static double elenaiDodgeBaseCost = 10.0;

    @Config.Name("elenai_weight_source_integration")
    @Config.Comment("With Elenai Dodge 2 installed: if true, Effective Weight comes from Elenai's armor weight (its config and formula, computed on the server) instead of JASS's armor_weight list.")
    public static boolean elenaiWeightSourceIntegration = true;

    @Config.Name("elenai_weight_conversion")
    @Config.Comment("JASS weight per Elenai weight unit. Full diamond is 16 Elenai units: 16 x 1.4 = 22.4.")
    @Config.RangeDouble(min = 0.0, max = 1000.0)
    public static double elenaiWeightConversion = 1.4;

    @Config.Name("debug_logging")
    @Config.Comment("Write one server log line for each action denied for Stamina (rate-limited). For testing; off for normal play.")
    public static boolean debugLogging = false;

    @Config.Name("command_permission_level")
    @Config.Comment("Permission level required for the /stamina command (2 = operators).")
    @Config.RangeInt(min = 0, max = 4)
    public static int commandPermissionLevel = 2;
}
