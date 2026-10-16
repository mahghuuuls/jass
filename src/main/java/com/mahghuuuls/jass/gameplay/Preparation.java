package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.api.PreparationResult;
import com.mahghuuuls.jass.api.PreparedStaminaAction;
import com.mahghuuuls.jass.api.SettlementOutcome;
import com.mahghuuuls.jass.api.SettlementResult;
import com.mahghuuuls.jass.api.StaminaPublicState;
import com.mahghuuuls.jass.core.ClaimLedger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

/**
 * One prepared action (REQ-088): the first half of every Stamina action. ActionGate creates it,
 * settles or releases it, and owns every rule; this object only remembers what prepare decided and
 * how the action was resolved, so a repeated settle or release changes nothing.
 */
final class Preparation implements PreparedStaminaAction {

    private final ActionGate gate;
    final EntityPlayer player;
    final ResourceLocation action;
    final boolean continuous;
    final ItemStack stack;
    final EnumHand hand;
    /** True for an addon preparation through the API; direct actions log and notify as before. */
    final boolean viaApi;
    final StaminaSession session;
    final ClaimLedger.Claim claim;
    final PreparationResult result;
    final double finalCost;

    private SettlementResult settlement;
    private boolean released;

    Preparation(ActionGate gate, EntityPlayer player, ResourceLocation action, boolean continuous, ItemStack stack,
            EnumHand hand, boolean viaApi, StaminaSession session, ClaimLedger.Claim claim, PreparationResult result,
            double finalCost) {
        this.gate = gate;
        this.player = player;
        this.action = action;
        this.continuous = continuous;
        this.stack = stack;
        this.hand = hand;
        this.viaApi = viaApi;
        this.session = session;
        this.claim = claim;
        this.result = result;
        this.finalCost = finalCost;
    }

    /** A refusal without a session: nothing recorded, nothing to settle. */
    static Preparation refusedWithoutSession(ActionGate gate, EntityPlayer player, ResourceLocation action,
            boolean continuous) {
        return new Preparation(gate, player, action, continuous, ItemStack.EMPTY, null, true, null, null,
                PreparationResult.REFUSED, 0.0);
    }

    @Override
    public PreparationResult getResult() {
        return result;
    }

    @Override
    public double getFinalCost() {
        return result == PreparationResult.ACCEPTED ? (claim != null ? claim.amount() : finalCost) : 0.0;
    }

    @Override
    public boolean isValid() {
        return gate.isValid(this);
    }

    @Override
    public SettlementResult settle() {
        return gate.settle(this);
    }

    @Override
    public void release() {
        gate.release(this);
    }

    boolean resolved() {
        return settlement != null || released;
    }

    SettlementResult settlement() {
        return settlement;
    }

    void recordSettlement(SettlementResult value) {
        settlement = value;
    }

    void markReleased() {
        released = true;
    }

    /** A result that pays nothing, for refused, released, expired, or invalidated preparations. */
    static SettlementResult nothingPaid(SettlementOutcome outcome, StaminaPublicState state) {
        return new SettlementResult(outcome, 0.0, state);
    }
}
