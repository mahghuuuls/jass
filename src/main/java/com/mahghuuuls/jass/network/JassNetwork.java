package com.mahghuuuls.jass.network;

import com.mahghuuuls.jass.Tags;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** The {@code jass} channel and its messages. */
public final class JassNetwork {

    /** Receives server snapshots on the client. Installed by the client proxy; never set on a dedicated server. */
    public interface ClientSnapshotSink {
        void onSnapshot(StaminaSnapshotMessage message);
    }

    /** Receives client input facts, which it must validate, on the server. Installed by the gameplay layer. */
    public interface ServerInputSink {
        void onAirSwing(EntityPlayerMP player);
    }

    private static SimpleNetworkWrapper channel;
    private static volatile ClientSnapshotSink clientSink;
    private static volatile ServerInputSink serverSink;

    private JassNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
        channel.registerMessage(StaminaSnapshotMessage.Handler.class, StaminaSnapshotMessage.class, 0, Side.CLIENT);
        channel.registerMessage(AirSwingMessage.Handler.class, AirSwingMessage.class, 1, Side.SERVER);
    }

    public static SimpleNetworkWrapper channel() {
        return channel;
    }

    public static void setClientSink(ClientSnapshotSink sink) {
        clientSink = sink;
    }

    static ClientSnapshotSink clientSink() {
        return clientSink;
    }

    public static void setServerSink(ServerInputSink sink) {
        serverSink = sink;
    }

    static ServerInputSink serverSink() {
        return serverSink;
    }
}
