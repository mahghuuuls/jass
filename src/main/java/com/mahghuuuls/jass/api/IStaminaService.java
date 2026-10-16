package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.item.ItemStack;
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

    /** Base Stamina Regeneration per second; not scaled by a raised blocking item (REV-015). */
    double getStaminaRegeneration(EntityPlayer player);

    double getRegenerationDelay(EntityPlayer player);

    double getStaminaEfficiency(EntityPlayer player);

    /** Effective Weight: worn armor (from the active weight source) plus the items in both hands. */
    double getEffectiveWeight(EntityPlayer player);

    /** True while Inhibited gating suspends this player's Stamina costs (REQ-084). */
    boolean isSpendingGated(EntityPlayer player);

    /**
     * Removes {@code amount} directly, down to the debt floor. While a prepared continuous tick
     * reserves Stamina, spend stops above the reserve; the amount spent is
     * {@code -result.getActualDelta()} (REQ-089). Like {@link #restore} and {@link #set},
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
     * rules, never into debt. Unsuccessful when Available Stamina (Stamina minus open claims) is gone;
     * the caller stops its action then.
     */
    StaminaMutationResult drainContinuousTick(EntityPlayer player, ResourceLocation action, double costPerSecond,
            boolean movement);

    /**
     * Prepares a Discrete Action before its effect (REQ-088): the same exemption, gating, start rule,
     * efficiency, weight, minimum cost, and one cost event as {@link #tryDiscreteAction}, but nothing is
     * paid until {@link PreparedStaminaAction#settle()}. Run the effect only when the result is not
     * refused; settle on success, release otherwise. An accepted preparation holds a Stamina Claim
     * (REQ-089). Server thread only. Since API 1.1; JASS 1.0 does not have it.
     *
     * @param stack the item behind the action, or {@link ItemStack#EMPTY}
     * @param hand  the hand behind the action, or {@code null}
     */
    default PreparedStaminaAction prepareDiscreteAction(EntityPlayer player, ResourceLocation action, double baseCost,
            boolean movement, ItemStack stack, EnumHand hand) {
        throw new UnsupportedOperationException("prepared actions need JASS API 1.1 or later");
    }

    /**
     * Prepares one server tick of a Continuous Action before its effect (REQ-088, REQ-089): the same
     * rules and one cost event as {@link #drainContinuousTick}, but the tick's debit
     * ({@code costPerSecond / 20} after efficiency and weight) is only reserved: never more than the
     * Available Stamina above 0 (Stamina minus other open claims), so it never creates debt on its own.
     * Other actions treat the reserve as spent, and settling pays it in full. When less than the debit
     * was available, {@link PreparedStaminaAction#getFinalCost()} is the smaller reserve. Prepare once
     * per tick; settle after the tick's effect. A settlement of {@link SettlementOutcome#PAID_STOP}
     * means the reserve is paid and Stamina is gone: keep this tick's effect and stop the action.
     * Server thread only. Since API 1.1: on JASS 1.0 the call fails with {@code NoSuchMethodError}, so
     * check {@link JassApi#API_VERSION} first.
     *
     * @param stack the item behind the action, or {@link ItemStack#EMPTY}
     * @param hand  the hand behind the action, or {@code null}
     */
    default PreparedStaminaAction prepareContinuousTick(EntityPlayer player, ResourceLocation action,
            double costPerSecond, boolean movement, ItemStack stack, EnumHand hand) {
        throw new UnsupportedOperationException("prepared actions need JASS API 1.1 or later");
    }

    /** As {@link #tryDiscreteAction}, telling cost event listeners the item and hand (REQ-087). */
    default StaminaMutationResult tryDiscreteAction(EntityPlayer player, ResourceLocation action, double baseCost,
            boolean movement, ItemStack stack, EnumHand hand) {
        return tryDiscreteAction(player, action, baseCost, movement);
    }

    /** As {@link #drainContinuousTick}, telling cost event listeners the item and hand (REQ-087). */
    default StaminaMutationResult drainContinuousTick(EntityPlayer player, ResourceLocation action,
            double costPerSecond, boolean movement, ItemStack stack, EnumHand hand) {
        return drainContinuousTick(player, action, costPerSecond, movement);
    }
}
