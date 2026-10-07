package com.mahghuuuls.jass.config;

/** Immutable copy of the server gameplay settings taken when configuration loads or changes. */
public final class ServerSettings {

    private final int revision;
    private final double maxStamina;
    private final double staminaRegeneration;
    private final double regenerationDelay;
    private final double maxDebtFraction;
    private final double efficiencyScale;
    private final double minDiscreteCost;
    private final boolean creativeConsumption;
    private final int commandPermissionLevel;
    private final double sprintCost;
    private final boolean jumpCostEnabled;
    private final double jumpCost;
    private final double weightFactor;
    private final ItemValueRules armorWeights;
    private final double fallbackWeightPerArmorPoint;
    private final double defaultMeleeCost;
    private final ItemValueRules meleeCosts;
    private final double defaultBowDrawCost;
    private final ItemValueRules bowDrawCosts;
    private final boolean debugLogging;

    ServerSettings(int revision) {
        this.revision = revision;
        this.maxStamina = JassConfig.maxStamina;
        this.staminaRegeneration = JassConfig.staminaRegeneration;
        this.regenerationDelay = JassConfig.regenerationDelay;
        this.maxDebtFraction = ConfigValues.debtFraction(JassConfig.maxDebtFraction);
        this.efficiencyScale = JassConfig.efficiencyScale;
        this.minDiscreteCost = JassConfig.minDiscreteCost;
        this.creativeConsumption = JassConfig.creativeConsumption;
        this.commandPermissionLevel = JassConfig.commandPermissionLevel;
        this.sprintCost = JassConfig.sprintCost;
        this.jumpCostEnabled = JassConfig.jumpCostEnabled;
        this.jumpCost = JassConfig.jumpCost;
        this.weightFactor = JassConfig.weightFactor;
        this.armorWeights = ItemValueRules.parse("armor_weight", JassConfig.armorWeight, 0.0);
        this.fallbackWeightPerArmorPoint = JassConfig.fallbackWeightPerArmorPoint;
        this.defaultMeleeCost = JassConfig.defaultMeleeCost;
        this.meleeCosts = ItemValueRules.parse("melee_cost_overrides", JassConfig.meleeCostOverrides, 0.0);
        this.defaultBowDrawCost = JassConfig.defaultBowDrawCost;
        this.bowDrawCosts = ItemValueRules.parse("bow_draw_cost_overrides", JassConfig.bowDrawCostOverrides, 0.0);
        this.debugLogging = JassConfig.debugLogging;
    }

    /** Increases each time configuration is reloaded, so caches can tell they are stale. */
    public int revision() {
        return revision;
    }

    public double maxStamina() {
        return maxStamina;
    }

    public double staminaRegeneration() {
        return staminaRegeneration;
    }

    public double regenerationDelay() {
        return regenerationDelay;
    }

    public double maxDebtFraction() {
        return maxDebtFraction;
    }

    public double efficiencyScale() {
        return efficiencyScale;
    }

    public double minDiscreteCost() {
        return minDiscreteCost;
    }

    public boolean creativeConsumption() {
        return creativeConsumption;
    }

    public int commandPermissionLevel() {
        return commandPermissionLevel;
    }

    /** Stamina drained per second of sprinting on land, before factors. */
    public double sprintCost() {
        return sprintCost;
    }

    /** True when ground jumps cost Stamina; the client learns it from the snapshot. */
    public boolean jumpCostEnabled() {
        return jumpCostEnabled;
    }

    /** Cost of one ground jump before factors. */
    public double jumpCost() {
        return jumpCost;
    }

    /** Movement cost multiplier per point of Effective Weight. */
    public double weightFactor() {
        return weightFactor;
    }

    /** Configured weight of the named worn armor item, or {@code NaN} when it has no entry. */
    public double armorWeight(String registryName, int metadata) {
        return armorWeights.valueFor(registryName, metadata, Double.NaN);
    }

    public double fallbackWeightPerArmorPoint() {
        return fallbackWeightPerArmorPoint;
    }

    /** Melee cost of one swing with the named item; the name and metadata come from the gameplay item-key rules. */
    public double meleeCost(String registryName, int metadata) {
        return meleeCosts.valueFor(registryName, metadata, defaultMeleeCost);
    }

    /** Stamina drained per second of drawing the named bow-draw item, before factors; 0 means free and never interrupted. */
    public double bowDrawCost(String registryName, int metadata) {
        return bowDrawCosts.valueFor(registryName, metadata, defaultBowDrawCost);
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    /** Every parsed per-item list, for warnings and unknown-item checks. */
    public ItemValueRules[] itemLists() {
        return new ItemValueRules[]{meleeCosts, bowDrawCosts, armorWeights};
    }
}
