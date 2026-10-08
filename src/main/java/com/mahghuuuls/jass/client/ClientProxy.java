package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.api.client.ClientStaminaApi;
import com.mahghuuuls.jass.CommonProxy;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraftforge.common.MinecraftForge;

/** Registers client-only listeners. Loaded only on the physical client. */
public final class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        FeedbackPlayer feedback = new FeedbackPlayer();
        ClientStaminaState state = new ClientStaminaState(feedback);
        JassNetwork.setClientSink(state);
        ClientStaminaApi.bind(ClientStaminaState::apiSnapshot);
        MinecraftForge.EVENT_BUS.register(state);
        MinecraftForge.EVENT_BUS.register(new HudPresenter(feedback));
        MinecraftForge.EVENT_BUS.register(ClientActionGuard.install(feedback));
    }
}
