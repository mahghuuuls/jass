package com.mahghuuuls.jass.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
    private final ItemValueRules heldWeights;
    private final double fallbackWeightPerArmorPoint;
    private final double defaultMeleeCost;
    private final ItemValueRules meleeCosts;
    private final double defaultBowDrawCost;
    private final ItemValueRules bowDrawCosts;
    private final double blockStaminaPerDamage;
    private final double stabilityScale;
    private final ItemValueRules shieldStability;
    private final double defaultShieldStability;
    private final ItemValueRules shieldMeleeBlock;
    private final ItemValueRules shieldProjectileBlock;
    private final double defaultShieldMeleeBlock;
    private final double defaultShieldProjectileBlock;
    private final double guardBreakCooldown;
    private final double shieldRaisedRegeneration;
    private final ItemModifierRules itemModifiers;
    private final boolean inhibitedIntegration;
    private final Set<String> disabledProviders;
    private final boolean rlcombatIntegration;
    private final boolean elenaiDodgeResourceIntegration;
    private final double elenaiDodgeBaseCost;
    private final boolean elenaiWeightSourceIntegration;
    private final double elenaiWeightConversion;
    private final boolean debugLogging;
    private final String[] armorWeightLines;
    private final String[] heldWeightLines;
    private final String[] meleeCostLines;
    private final String[] bowDrawCostLines;
    private final String[] shieldStabilityLines;
    private final String[] shieldMeleeBlockLines;
    private final String[] shieldProjectileBlockLines;
    private final String[] itemModifierLines;

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
        this.heldWeights = ItemValueRules.parse("held_weight", JassConfig.heldWeight, 0.0);
        this.fallbackWeightPerArmorPoint = JassConfig.fallbackWeightPerArmorPoint;
        this.defaultMeleeCost = JassConfig.defaultMeleeCost;
        this.meleeCosts = ItemValueRules.parse("melee_cost_overrides", JassConfig.meleeCostOverrides, 0.0);
        this.defaultBowDrawCost = JassConfig.defaultBowDrawCost;
        this.bowDrawCosts = ItemValueRules.parse("bow_draw_cost_overrides", JassConfig.bowDrawCostOverrides, 0.0);
        this.blockStaminaPerDamage = JassConfig.blockStaminaPerDamage;
        this.stabilityScale = JassConfig.stabilityScale;
        this.shieldStability = ItemValueRules.parse("shield_stability", JassConfig.shieldStability, 0.0);
        this.defaultShieldStability = JassConfig.defaultShieldStability;
        this.shieldMeleeBlock = ItemValueRules.parse("shield_melee_block", JassConfig.shieldMeleeBlock, 0.0, 1.0);
        this.shieldProjectileBlock = ItemValueRules.parse("shield_projectile_block", JassConfig.shieldProjectileBlock,
                0.0, 1.0);
        this.defaultShieldMeleeBlock = JassConfig.defaultShieldMeleeBlock;
        this.defaultShieldProjectileBlock = JassConfig.defaultShieldProjectileBlock;
        this.guardBreakCooldown = JassConfig.guardBreakCooldown;
        this.shieldRaisedRegeneration = JassConfig.shieldRaisedRegeneration;
        this.itemModifiers = ItemModifierRules.parse("item_stamina_modifiers", JassConfig.itemStaminaModifiers);
        this.inhibitedIntegration = JassConfig.inhibitedIntegration;
        Set<String> disabled = new HashSet<>();
        for (String id : JassConfig.disabledStaminaProviders) {
            if (id != null && !id.trim().isEmpty()) {
                disabled.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }
        this.disabledProviders = Collections.unmodifiableSet(disabled);
        this.rlcombatIntegration = JassConfig.rlcombatIntegration;
        this.elenaiDodgeResourceIntegration = JassConfig.elenaiDodgeResourceIntegration;
        this.elenaiDodgeBaseCost = JassConfig.elenaiDodgeBaseCost;
        this.elenaiWeightSourceIntegration = JassConfig.elenaiWeightSourceIntegration;
        this.elenaiWeightConversion = JassConfig.elenaiWeightConversion;
        this.debugLogging = JassConfig.debugLogging;
        this.armorWeightLines = JassConfig.armorWeight.clone();
        this.heldWeightLines = JassConfig.heldWeight.clone();
        this.meleeCostLines = JassConfig.meleeCostOverrides.clone();
        this.bowDrawCostLines = JassConfig.bowDrawCostOverrides.clone();
        this.shieldStabilityLines = JassConfig.shieldStability.clone();
        this.shieldMeleeBlockLines = JassConfig.shieldMeleeBlock.clone();
        this.shieldProjectileBlockLines = JassConfig.shieldProjectileBlock.clone();
        this.itemModifierLines = JassConfig.itemStaminaModifiers.clone();
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



    public double blockStaminaPerDamage() {
        return blockStaminaPerDamage;
    }

    public double stabilityScale() {
        return stabilityScale;
    }




    /** Guard Break length in seconds. */
    public double guardBreakCooldown() {
        return guardBreakCooldown;
    }

    /** Share of Stamina Regeneration kept while a blocking item is raised (REV-015). */
    public double shieldRaisedRegeneration() {
        return shieldRaisedRegeneration;
    }

    /** Configured Item Stamina Modifier rules. */
    public ItemModifierRules itemModifiers() {
        return itemModifiers;
    }

    public boolean inhibitedIntegration() {
        return inhibitedIntegration;
    }

    /** True when the server configuration disables this addon provider id (REQ-082). */
    public boolean providerDisabled(String providerId) {
        return disabledProviders.contains(providerId.toLowerCase(Locale.ROOT));
    }

    public boolean rlcombatIntegration() {
        return rlcombatIntegration;
    }

    public boolean elenaiDodgeResourceIntegration() {
        return elenaiDodgeResourceIntegration;
    }

    public double elenaiDodgeBaseCost() {
        return elenaiDodgeBaseCost;
    }

    public boolean elenaiWeightSourceIntegration() {
        return elenaiWeightSourceIntegration;
    }

    public double elenaiWeightConversion() {
        return elenaiWeightConversion;
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    /** Every parsed per-item list, for warnings and unknown-item checks. */
    public ItemList[] itemLists() {
        return new ItemList[]{itemModifiers, meleeCosts, bowDrawCosts, armorWeights, heldWeights, shieldStability, shieldMeleeBlock,
                shieldProjectileBlock};
    }

    // The raw per-item lines and defaults, for the client's tooltips (REV-010): the client parses
    // them with the same rule classes the server uses.

    public double defaultMeleeCost() {
        return defaultMeleeCost;
    }

    public double defaultBowDrawCost() {
        return defaultBowDrawCost;
    }

    public double defaultShieldStability() {
        return defaultShieldStability;
    }

    public double defaultShieldMeleeBlock() {
        return defaultShieldMeleeBlock;
    }

    public double defaultShieldProjectileBlock() {
        return defaultShieldProjectileBlock;
    }

    public String[] armorWeightLines() {
        return armorWeightLines.clone();
    }

    public String[] heldWeightLines() {
        return heldWeightLines.clone();
    }

    public String[] meleeCostLines() {
        return meleeCostLines.clone();
    }

    public String[] bowDrawCostLines() {
        return bowDrawCostLines.clone();
    }

    public String[] shieldStabilityLines() {
        return shieldStabilityLines.clone();
    }

    public String[] shieldMeleeBlockLines() {
        return shieldMeleeBlockLines.clone();
    }

    public String[] shieldProjectileBlockLines() {
        return shieldProjectileBlockLines.clone();
    }

    public String[] itemModifierLines() {
        return itemModifierLines.clone();
    }

    // Configured values only, NaN when the item has no entry (REQ-086: configuration, then addon
    // providers, then the default).

    public double configuredMeleeCost(String registryName, int metadata) {
        return meleeCosts.valueFor(registryName, metadata, Double.NaN);
    }

    public double configuredHeldWeight(String registryName, int metadata) {
        return heldWeights.valueFor(registryName, metadata, Double.NaN);
    }

    public double configuredBowDrawCost(String registryName, int metadata) {
        return bowDrawCosts.valueFor(registryName, metadata, Double.NaN);
    }

    public double configuredShieldStability(String registryName, int metadata) {
        return shieldStability.valueFor(registryName, metadata, Double.NaN);
    }

    public double configuredShieldMeleeBlock(String registryName, int metadata) {
        return shieldMeleeBlock.valueFor(registryName, metadata, Double.NaN);
    }

    public double configuredShieldProjectileBlock(String registryName, int metadata) {
        return shieldProjectileBlock.valueFor(registryName, metadata, Double.NaN);
    }
}
