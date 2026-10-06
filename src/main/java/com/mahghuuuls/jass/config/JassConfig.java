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

    @Config.Name("debug_logging")
    @Config.Comment("Write one server log line for each action denied for Stamina (rate-limited). For testing; off for normal play.")
    public static boolean debugLogging = false;

    @Config.Name("command_permission_level")
    @Config.Comment("Permission level required for the /stamina command (2 = operators).")
    @Config.RangeInt(min = 0, max = 4)
    public static int commandPermissionLevel = 2;
}
