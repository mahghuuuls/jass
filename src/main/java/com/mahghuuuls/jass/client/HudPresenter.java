package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.JustAnotherStaminaSystemMod;
import com.mahghuuuls.jass.api.client.StaminaHudRenderEvent;
import com.mahghuuuls.jass.client.JassClientConfig.HudStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Decides each frame whether a Stamina display is drawn and which one, and applies the shared
 * visibility rules (ARC-013). Renderers only draw what they are handed; the Classic Bar bar asks
 * {@link #shown()} and {@link #flashing()} so it follows the same rules.
 *
 * <p>Drawn just before the air row, so the default bar sits in the row directly above the food row
 * (the air bubbles stack above it) and under chat and the player list. Classic Bar advances Forge's
 * column heights for each bar it draws, so the built-in display stacks after Classic Bar's bars.
 */
public final class HudPresenter {

    private final BarHud bar = new BarHud();
    private final SegmentHud segments = new SegmentHud();
    private final FeedbackPlayer feedback;
    private HudDisplayProvider provider = HudDisplayProvider.NONE;

    HudPresenter(FeedbackPlayer feedback) {
        this.feedback = feedback;
    }

    /** Set once at start-up when an optional HUD mod is present. */
    public void useProvider(HudDisplayProvider provider) {
        this.provider = provider;
    }

    /**
     * True when a Stamina display should show now: Stamina is known and neither hiding rule applies
     * (full Stamina, or outside the Inhibited effect while gating is active), or a denial flash runs.
     */
    public boolean shown() {
        if (!ClientStaminaState.received() || ClientStaminaState.maximum() <= 0.0F) {
            return false;
        }
        boolean flashActive = feedback.flashActive();
        if (JassClientConfig.hudHideWhenFull && ClientStaminaState.visible() >= ClientStaminaState.maximum()
                && !flashActive) {
            return false;
        }
        return !(JassClientConfig.hudHideOutsideInhibited && ClientStaminaState.gated() && !flashActive);
    }

    /** True in the lit phases of a denial flash. */
    public boolean flashing() {
        return feedback.flashing();
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
        if (provider.drawsStamina()) {
            return; // Classic Bar draws the Stamina bar; one display at a time (REQ-055).
        }
        ScaledResolution scaled = event.getResolution();
        if (MinecraftForge.EVENT_BUS.post(new StaminaHudRenderEvent(ClientStaminaState.apiSnapshot(),
                event.getPartialTicks(), scaled.getScaledWidth(), scaled.getScaledHeight()))) {
            return; // An addon replaced every built-in display (REQ-085).
        }
        if (!shown()) {
            return;
        }
        float visible = ClientStaminaState.visible();
        float maximum = ClientStaminaState.maximum();
        if (JassClientConfig.hudStyle == HudStyle.SEGMENTS) {
            int[] row = BarLayout.segments(JassClientConfig.segmentsXOffset, JassClientConfig.segmentsYOffset,
                    scaled.getScaledWidth(), scaled.getScaledHeight(), GuiIngameForge.right_height);
            GuiIngameForge.right_height = row[2];
            segments.draw(mc, row[0], row[1], visible, maximum, JassClientConfig.hudNumericText, feedback.flashing());
            return;
        }
        BarLayout layout = BarLayout.compute(JassClientConfig.barPosition, JassClientConfig.barWidth,
                JassClientConfig.barHeight, JassClientConfig.barXOffset, JassClientConfig.barYOffset,
                scaled.getScaledWidth(), scaled.getScaledHeight(), GuiIngameForge.right_height,
                GuiIngameForge.left_height);
        GuiIngameForge.right_height = layout.rightHeight;
        GuiIngameForge.left_height = layout.leftHeight;
        bar.draw(mc, layout, visible, maximum, JassClientConfig.hudNumericText, JassClientConfig.barFillDirection,
                feedback.flashing());
    }

    /** One line per world join naming the active display (IMP-020 diagnostics). */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote || event.getEntity() != Minecraft.getMinecraft().player) {
            return;
        }
        String display = provider.drawsStamina() ? "Classic Bar bar (jassstamina)"
                : JassClientConfig.hudStyle == HudStyle.SEGMENTS ? "built-in segments" : "built-in bar";
        JustAnotherStaminaSystemMod.LOGGER.info("JASS Stamina display: {}", display);
    }
}
