package com.mahghuuuls.jass.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client-to-server: the client blocked an attack click because its last snapshot said the
 * player cannot spend. Carries no data and is never charged; the server records a denial only
 * if it agrees the player cannot start an action.
 */
public final class RefusedAttackMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static final class Handler implements IMessageHandler<RefusedAttackMessage, IMessage> {

        @Override
        public IMessage onMessage(RefusedAttackMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                JassNetwork.ServerInputSink sink = JassNetwork.serverSink();
                if (sink != null) {
                    sink.onRefusedAttack(player);
                }
            });
            return null;
        }
    }
}
