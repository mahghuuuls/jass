package com.mahghuuuls.jass.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The open Stamina Claims of one player (REQ-089). A claim holds Stamina that an accepted prepared
 * action will pay, so every other start rule and drain sees it as already spent. Plain Java on
 * purpose: callers pass Stamina in, so the bookkeeping is tested without Minecraft. Server thread
 * only.
 */
public final class ClaimLedger {

    public enum State {
        OPEN,
        SETTLED,
        RELEASED,
        EXPIRED,
        INVALIDATED
    }

    /** One claim: a discrete cost, or a continuous tick's reserve. */
    public static final class Claim {
        private final boolean continuous;
        private double amount;
        private State state = State.OPEN;

        private Claim(boolean continuous, double amount) {
            this.continuous = continuous;
            this.amount = amount;
        }

        public boolean isContinuous() {
            return continuous;
        }

        /** Stamina held: the discrete final cost, or the continuous reserve. */
        public double amount() {
            return amount;
        }

        public State state() {
            return state;
        }

        public boolean isOpen() {
            return state == State.OPEN;
        }
    }

    /** Usually empty; a claim lives at most until the end of the server tick that made it. */
    private final List<Claim> open = new ArrayList<>(2);

    /** Opens a claim, provisional until {@link #reconcile}d. Negative amounts count as 0. */
    public Claim register(boolean continuous, double amount) {
        Claim claim = new Claim(continuous, Math.max(0.0, amount));
        open.add(claim);
        return claim;
    }

    /** Sets an open claim's amount once its final cost is known. */
    public void reconcile(Claim claim, double amount) {
        if (claim.isOpen()) {
            claim.amount = Math.max(0.0, amount);
        }
    }

    /** Stamina minus every open claim (REQ-089). */
    public double available(double stamina) {
        return stamina - total(null);
    }

    /** Stamina minus every open claim except {@code except}. */
    public double availableExcept(double stamina, Claim except) {
        return stamina - total(except);
    }

    /** Stamina held by open continuous claims; API spend stops above it. */
    public double reserved() {
        double sum = 0.0;
        for (Claim claim : open) {
            if (claim.continuous) {
                sum += claim.amount;
            }
        }
        return sum;
    }

    /**
     * How much of an API spend of {@code amount} may be taken from {@code stamina} (REQ-089): all of
     * it while no continuous reserve is open; otherwise never so much that Stamina drops below the
     * reserves, and nothing when Stamina is already at or below them.
     */
    public double spendable(double amount, double stamina) {
        double wanted = Math.max(0.0, amount);
        double reserved = reserved();
        return reserved > 0.0 ? Math.min(wanted, Math.max(0.0, stamina - reserved)) : wanted;
    }

    /**
     * Ends an open claim. Returns false, changing nothing, when the claim was already resolved, so
     * a repeated settle or release cannot act twice.
     */
    public boolean resolve(Claim claim, State to) {
        if (!claim.isOpen() || to == State.OPEN || !open.remove(claim)) {
            return false;
        }
        claim.state = to;
        return true;
    }

    /** Ends every open claim; returns how many there were. */
    public int resolveAll(State to) {
        if (to == State.OPEN) {
            return 0;
        }
        int count = open.size();
        for (Claim claim : open) {
            claim.state = to;
        }
        open.clear();
        return count;
    }

    public boolean isEmpty() {
        return open.isEmpty();
    }

    /** A continuous tick's reserve: its cost, but never more than the Available Stamina above 0, so it never makes debt. */
    public static double reserve(double cost, double available) {
        return Math.max(0.0, Math.min(Math.max(0.0, cost), available));
    }

    private double total(Claim except) {
        double sum = 0.0;
        for (Claim claim : open) {
            if (claim != except) {
                sum += claim.amount;
            }
        }
        return sum;
    }
}
