package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Server-side Stamina operations for addons, from {@link JassApi#getStaminaService()}. Reads work on
 * the logical server; mutations must be called on the server thread. Causes and action ids use the
 * addon's own namespace.
 */
public interface IStaminaService {

    /** Current state, or {@code null} for a player without Stamina (not logged in, fake players). */
    StaminaPublicState getState(EntityPlayer player);

    double getMaximumStamina(EntityPlayer player);

    double getStaminaRegeneration(EntityPlayer player);

    double getRegenerationDelay(EntityPlayer player);

    double getStaminaEfficiency(EntityPlayer player);

    double getEffectiveWeight(EntityPlayer player);

    /** True while Inhibited gating suspends this player's Stamina costs (REQ-084). */
    boolean isSpendingGated(EntityPlayer player);

    /**
     * Removes {@code amount} directly, down to the debt floor. Like {@link #restore} and {@link #set},
     * this is a direct mutation: no start rule, efficiency, cost event, Creative exemption, or Inhibited
     * gating. For an action the player performs, use {@link #tryDiscreteAction} or
     * {@link #drainContinuousTick}, which follow every rule.
     */
    StaminaMutationResult spend(EntityPlayer player, double amount, ResourceLocation cause);

    /** Adds {@code amount}, up to Maximum Stamina. */
    StaminaMutationResult restore(EntityPlayer player, double amount, ResourceLocation cause);

    /** Sets internal Stamina, clamped to the debt floor and Maximum Stamina. */
    StaminaMutationResult set(EntityPlayer player, double value, ResourceLocation cause);

    /**
     * An addon Discrete Action following every JASS rule: refused at 0 or below, efficiency, minimum
     * cost, the cost event, debt, Creative, and gating. {@code movement} applies the weight multiplier.
     */
    StaminaMutationResult tryDiscreteAction(EntityPlayer player, ResourceLocation action, double baseCost,
            boolean movement);

    /**
     * One server tick of an addon Continuous Action: drains {@code costPerSecond / 20} with the same
     * rules, never into debt. Unsuccessful when Stamina is gone; the caller stops its action then.
     */
    StaminaMutationResult drainContinuousTick(EntityPlayer player, ResourceLocation action, double costPerSecond,
            boolean movement);
}
