package com.mahghuuuls.jass.api.client;

import net.minecraft.item.ItemStack;

import java.util.function.Function;
import java.util.function.Supplier;

/** Client-side, read-only Stamina access for addons (REQ-085). */
public final class ClientStaminaApi {

    private static volatile Supplier<ClientStaminaSnapshot> source = () -> ClientStaminaSnapshot.NONE;
    private static volatile boolean bound;
    private static volatile Function<ItemStack, ItemStaminaInfo> itemInfo = stack -> ItemStaminaInfo.NONE;
    private static volatile boolean itemInfoBound;

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

    /**
     * The Stamina values of an item as the server configures them (REQ-058), for tooltips drawn by
     * other mods; {@link ItemStaminaInfo#NONE} before the server's values arrive.
     */
    public static ItemStaminaInfo getItemInfo(ItemStack stack) {
        return itemInfo.apply(stack);
    }

    /** Called once by JASS itself on the client; a second call throws. */
    public static synchronized void bindItemInfo(Function<ItemStack, ItemStaminaInfo> source) {
        if (itemInfoBound) {
            throw new IllegalStateException("JASS's item info source is already bound");
        }
        itemInfoBound = true;
        itemInfo = source;
    }
}
