package com.mahghuuuls.jass.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server-to-client: what the owning player's client needs to draw and predict Stamina. */
public final class StaminaSnapshotMessage implements IMessage {

    private float visible;
    private float maximum;
    private boolean canSpend;
    private int denialCount;
    private boolean jumpCostEnabled;

    public StaminaSnapshotMessage() {
    }

    public StaminaSnapshotMessage(float visible, float maximum, boolean canSpend, int denialCount,
            boolean jumpCostEnabled) {
        this.visible = visible;
        this.maximum = maximum;
        this.canSpend = canSpend;
        this.denialCount = denialCount;
        this.jumpCostEnabled = jumpCostEnabled;
    }

    public float visible() {
        return visible;
    }

    public float maximum() {
        return maximum;
    }

    public boolean canSpend() {
        return canSpend;
    }

    /** Denials since login; a change tells the client to flash. */
    public int denialCount() {
        return denialCount;
    }

    /** True when the server charges ground jumps, so the client reports jumps and refuses them at zero. */
    public boolean jumpCostEnabled() {
        return jumpCostEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        visible = buf.readFloat();
        maximum = buf.readFloat();
        canSpend = buf.readBoolean();
        denialCount = buf.readInt();
        jumpCostEnabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(visible);
        buf.writeFloat(maximum);
        buf.writeBoolean(canSpend);
        buf.writeInt(denialCount);
        buf.writeBoolean(jumpCostEnabled);
    }

    public static final class Handler implements IMessageHandler<StaminaSnapshotMessage, IMessage> {

        @Override
        public IMessage onMessage(StaminaSnapshotMessage message, MessageContext ctx) {
            JassNetwork.ClientSnapshotSink sink = JassNetwork.clientSink();
            if (sink != null) {
                sink.onSnapshot(message);
            }
            return null;
        }
    }
}
