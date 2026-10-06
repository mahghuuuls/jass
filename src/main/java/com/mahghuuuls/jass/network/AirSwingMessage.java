package com.mahghuuuls.jass.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client-to-server: the player swung at air (the server cannot see misses). Carries no data;
 * the server decides whether and what to charge.
 */
public final class AirSwingMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static final class Handler implements IMessageHandler<AirSwingMessage, IMessage> {

        @Override
        public IMessage onMessage(AirSwingMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                JassNetwork.ServerInputSink sink = JassNetwork.serverSink();
                if (sink != null) {
                    sink.onAirSwing(player);
                }
            });
            return null;
        }
    }
}
