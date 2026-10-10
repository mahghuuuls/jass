package com.mahghuuuls.jass.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-to-client: the server's per-item configuration, as the same text lines and numbers the
 * server reads, so the client's tooltips show exactly the server's values (REQ-058, REV-010).
 * Sent at login and after a server configuration reload.
 */
public final class ItemValuesMessage implements IMessage {

    /** Every number the tooltips need, in a fixed order. */
    public enum Number {
        DEFAULT_MELEE_COST,
        DEFAULT_BOW_DRAW_COST,
        BLOCK_STAMINA_PER_DAMAGE,
        STABILITY_SCALE,
        DEFAULT_SHIELD_STABILITY,
        DEFAULT_SHIELD_MELEE_BLOCK,
        DEFAULT_SHIELD_PROJECTILE_BLOCK,
        FALLBACK_WEIGHT_PER_ARMOR_POINT,
        ELENAI_WEIGHT_CONVERSION
    }

    /** Every per-item list the tooltips need, in a fixed order. */
    public enum Lines {
        MELEE_COST_OVERRIDES,
        BOW_DRAW_COST_OVERRIDES,
        SHIELD_STABILITY,
        SHIELD_MELEE_BLOCK,
        SHIELD_PROJECTILE_BLOCK,
        ARMOR_WEIGHT,
        ITEM_STAMINA_MODIFIERS,
        ELENAI_WEIGHT_OVERRIDES
    }

    /** Guards against a broken packet allocating huge arrays. */
    private static final int MAX_LINES = 4096;

    private double[] numbers = new double[Number.values().length];
    private String[][] lines = new String[Lines.values().length][];
    private boolean elenaiWeightSource;

    public ItemValuesMessage() {
        for (int i = 0; i < lines.length; i++) {
            lines[i] = new String[0];
        }
    }

    public ItemValuesMessage set(Number key, double value) {
        numbers[key.ordinal()] = value;
        return this;
    }

    public ItemValuesMessage set(Lines key, String[] value) {
        lines[key.ordinal()] = value == null ? new String[0] : value.clone();
        return this;
    }

    /** True when the server takes Effective Weight from Elenai Dodge 2 (REQ-071). */
    public ItemValuesMessage elenaiWeightSource(boolean value) {
        elenaiWeightSource = value;
        return this;
    }

    public double get(Number key) {
        return numbers[key.ordinal()];
    }

    public String[] get(Lines key) {
        return lines[key.ordinal()].clone();
    }

    public boolean elenaiWeightSource() {
        return elenaiWeightSource;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = buf.readDouble();
        }
        for (int i = 0; i < lines.length; i++) {
            int count = Math.min(buf.readInt(), MAX_LINES);
            String[] list = new String[Math.max(0, count)];
            for (int j = 0; j < list.length; j++) {
                list[j] = ByteBufUtils.readUTF8String(buf);
            }
            lines[i] = list;
        }
        elenaiWeightSource = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        for (double number : numbers) {
            buf.writeDouble(number);
        }
        for (String[] list : lines) {
            int count = Math.min(list.length, MAX_LINES);
            buf.writeInt(count);
            for (int j = 0; j < count; j++) {
                ByteBufUtils.writeUTF8String(buf, list[j] == null ? "" : list[j]);
            }
        }
        buf.writeBoolean(elenaiWeightSource);
    }

    public static final class Handler implements IMessageHandler<ItemValuesMessage, IMessage> {

        @Override
        public IMessage onMessage(ItemValuesMessage message, MessageContext ctx) {
            JassNetwork.ClientItemValuesSink sink = JassNetwork.itemValuesSink();
            if (sink != null) {
                sink.onItemValues(message); // The sink moves the work to the client thread.
            }
            return null;
        }
    }
}
