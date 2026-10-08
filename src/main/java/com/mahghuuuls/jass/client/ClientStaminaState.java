package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.network.JassNetwork;
import com.mahghuuuls.jass.network.StaminaSnapshotMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * The last snapshot the server sent for the local player. The client draws and predicts only
 * from this; it never computes Stamina itself. Read and written on the client thread.
 */
public final class ClientStaminaState implements JassNetwork.ClientSnapshotSink {

    private static boolean received;
    private static float visible;
    private static float maximum;
    private static boolean canSpend = true;
    private static int denialCount;
    private static boolean jumpCostEnabled;
    private static boolean guardBroken;

    private final FeedbackPlayer feedback;

    ClientStaminaState(FeedbackPlayer feedback) {
        this.feedback = feedback;
    }

    public static boolean received() {
        return received;
    }

    public static float visible() {
        return visible;
    }

    public static float maximum() {
        return maximum;
    }

    public static boolean canSpend() {
        return canSpend;
    }

    /** True while Guard Break lasts. */
    public static boolean guardBroken() {
        return guardBroken;
    }

    /** True when the server charges ground jumps. */
    public static boolean jumpCostEnabled() {
        return jumpCostEnabled;
    }

    @Override
    public void onSnapshot(StaminaSnapshotMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            boolean newDenial = received && message.denialCount() != denialCount;
            received = true;
            visible = message.visible();
            maximum = message.maximum();
            canSpend = message.canSpend();
            denialCount = message.denialCount();
            jumpCostEnabled = message.jumpCostEnabled();
            guardBroken = message.guardBroken();
            if (newDenial) {
                feedback.onDenied();
            }
        });
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            received = false;
            visible = 0.0F;
            maximum = 0.0F;
            canSpend = true;
            denialCount = 0;
            jumpCostEnabled = false;
            guardBroken = false;
        });
    }
}
