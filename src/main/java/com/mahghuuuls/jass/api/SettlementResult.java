package com.mahghuuuls.jass.api;

/** The result of {@link PreparedStaminaAction#settle()}. Immutable. */
public final class SettlementResult {

    private final SettlementOutcome outcome;
    private final double amountPaid;
    private final StaminaPublicState newState;

    public SettlementResult(SettlementOutcome outcome, double amountPaid, StaminaPublicState newState) {
        this.outcome = outcome;
        this.amountPaid = amountPaid;
        this.newState = newState;
    }

    public SettlementOutcome getOutcome() {
        return outcome;
    }

    /** Stamina actually removed; 0 unless the outcome is paid. */
    public double getAmountPaid() {
        return amountPaid;
    }

    /** The player's state after settling, or {@code null} when the player has no session. */
    public StaminaPublicState getNewState() {
        return newState;
    }

    /** True for {@link SettlementOutcome#PAID_STOP}: stop the continuous action after this tick. */
    public boolean mustStop() {
        return outcome == SettlementOutcome.PAID_STOP;
    }
}
