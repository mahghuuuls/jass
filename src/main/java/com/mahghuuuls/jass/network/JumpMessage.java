package com.mahghuuuls.jass.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client-to-server: the player performed a jump from the ground (the server cannot tell jumps
 * from knockback). Carries no data; the server validates it and decides the cost.
 */
public final class JumpMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static final class Handler implements IMessageHandler<JumpMessage, IMessage> {

        @Override
        public IMessage onMessage(JumpMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                JassNetwork.ServerJumpSink sink = JassNetwork.jumpSink();
                if (sink != null) {
                    sink.onJump(player);
                }
            });
            return null;
        }
    }
}
