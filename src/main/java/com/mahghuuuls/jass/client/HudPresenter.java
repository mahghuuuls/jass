package com.mahghuuuls.jass.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import com.mahghuuuls.jass.api.client.StaminaHudRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Decides each frame whether a Stamina display is drawn and which one, and applies the shared
 * visibility rules. Renderers only draw what they are handed.
 *
 * <p>Drawn just before the air row, so the bar sits in a fixed row directly above the food row
 * (the air bubbles stack above it) and under chat and the player list.
 */
public final class HudPresenter {

    private final BarHud bar = new BarHud();
    private final FeedbackPlayer feedback;

    HudPresenter(FeedbackPlayer feedback) {
        this.feedback = feedback;
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.AIR) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.playerController == null || !mc.playerController.shouldDrawHUD()) {
            return;
        }
        if (!ClientStaminaState.received() || ClientStaminaState.maximum() <= 0.0F) {
            return;
        }
        ScaledResolution scaled = event.getResolution();
        if (MinecraftForge.EVENT_BUS.post(new StaminaHudRenderEvent(ClientStaminaState.apiSnapshot(),
                event.getPartialTicks(), scaled.getScaledWidth(), scaled.getScaledHeight()))) {
            return; // An addon replaced every built-in display (REQ-085).
        }
        float visible = ClientStaminaState.visible();
        float maximum = ClientStaminaState.maximum();
        boolean flashActive = feedback.flashActive();
        if (JassClientConfig.hudHideWhenFull && visible >= maximum && !flashActive) {
            return;
        }
        if (JassClientConfig.hudHideOutsideInhibited && ClientStaminaState.gated() && !flashActive) {
            return;
        }
        ScaledResolution resolution = event.getResolution();
        bar.draw(mc, resolution, visible, maximum, JassClientConfig.hudNumericText, feedback.flashing());
    }
}
