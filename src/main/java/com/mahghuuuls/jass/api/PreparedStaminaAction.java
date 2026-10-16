package com.mahghuuuls.jass.api;

/**
 * An action checked and priced before its effect (REQ-088). Prepare, run the effect only when the
 * result is not {@link PreparationResult#REFUSED}, then call {@link #settle()} once if the effect
 * succeeded or {@link #release()} if it did not. An accepted preparation holds a Stamina Claim, so
 * other actions treat its Stamina as spent (REQ-089). A preparation neither settled nor released by
 * the end of the server tick that created it is released. Server thread only.
 */
public interface PreparedStaminaAction {

    PreparationResult getResult();

    /** The final cost after efficiency, weight, minimum cost, and cost listeners; 0 unless accepted. For a continuous tick, the reserved debit. */
    double getFinalCost();

    /** True while settling would still pay (or, for a free preparation, still return free). Changes nothing. */
    boolean isValid();

    /**
     * Pays the claim. Called again, returns the first result and changes nothing. Never throws for a
     * failing change listener: the payment stays and the error is logged.
     */
    SettlementResult settle();

    /** Drops the claim with no charge, delay, or notification. Does nothing after settle or a first release. */
    void release();
}
