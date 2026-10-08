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
    private boolean guardBroken;
    private boolean gated;

    public StaminaSnapshotMessage() {
    }

    public StaminaSnapshotMessage(float visible, float maximum, boolean canSpend, int denialCount,
            boolean jumpCostEnabled, boolean guardBroken, boolean gated) {
        this.visible = visible;
        this.maximum = maximum;
        this.canSpend = canSpend;
        this.denialCount = denialCount;
        this.jumpCostEnabled = jumpCostEnabled;
        this.guardBroken = guardBroken;
        this.gated = gated;
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

    /** True while Guard Break lasts, so the client refuses to raise any blocking item. */
    public boolean guardBroken() {
        return guardBroken;
    }

    /** True while Inhibited gating suspends costs, so the HUD may hide. */
    public boolean gated() {
        return gated;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        visible = buf.readFloat();
        maximum = buf.readFloat();
        canSpend = buf.readBoolean();
        denialCount = buf.readInt();
        jumpCostEnabled = buf.readBoolean();
        guardBroken = buf.readBoolean();
        gated = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(visible);
        buf.writeFloat(maximum);
        buf.writeBoolean(canSpend);
        buf.writeInt(denialCount);
        buf.writeBoolean(jumpCostEnabled);
        buf.writeBoolean(guardBroken);
        buf.writeBoolean(gated);
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
