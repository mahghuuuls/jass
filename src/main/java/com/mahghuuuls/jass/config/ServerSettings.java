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
    private final double defaultMeleeCost;
    private final ItemValueRules meleeCosts;
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
        this.defaultMeleeCost = JassConfig.defaultMeleeCost;
        this.meleeCosts = ItemValueRules.parse("melee_cost_overrides", JassConfig.meleeCostOverrides, 0.0);
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

    /** Melee cost of one swing with the named item; the name and metadata come from the gameplay item-key rules. */
    public double meleeCost(String registryName, int metadata) {
        return meleeCosts.valueFor(registryName, metadata, defaultMeleeCost);
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    /** Every parsed per-item list, for warnings and unknown-item checks. */
    public ItemValueRules[] itemLists() {
        return new ItemValueRules[]{meleeCosts};
    }
}
