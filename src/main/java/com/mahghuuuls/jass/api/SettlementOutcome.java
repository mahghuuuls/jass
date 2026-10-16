package com.mahghuuuls.jass.api;

/** What settling a prepared action did (REQ-094). */
public enum SettlementOutcome {
    /** Paid; the amount is in {@link SettlementResult#getAmountPaid()}. */
    PAID,
    /**
     * Paid, and the paid continuous tick left Stamina at 0 or below: let this tick's effect stand,
     * then stop the action.
     */
    PAID_STOP,
    /** Nothing to pay: the preparation was free. */
    FREE,
    /**
     * Nothing paid: the claim was invalidated (logout, or Stamina set while it was open), already
     * released or expired, or the preparation was refused.
     */
    INVALIDATED
}
