package com.mahghuuuls.jass.api.client;

import java.util.function.Supplier;

/** Client-side, read-only Stamina access for addons (REQ-085). */
public final class ClientStaminaApi {

    private static volatile Supplier<ClientStaminaSnapshot> source = () -> ClientStaminaSnapshot.NONE;
    private static volatile boolean bound;

    private ClientStaminaApi() {
    }

    /** The local player's latest snapshot; {@link ClientStaminaSnapshot#NONE} before joining. */
    public static ClientStaminaSnapshot getSnapshot() {
        return source.get();
    }

    /** Called once by JASS itself on the client; a second call throws. */
    public static synchronized void bind(Supplier<ClientStaminaSnapshot> snapshots) {
        if (bound) {
            throw new IllegalStateException("JASS's client snapshot source is already bound");
        }
        bound = true;
        source = snapshots;
    }
}
