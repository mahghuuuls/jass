package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.NumericText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Draws the segment style: ten 9x9 icons from the {@code stamina_segments} atlas (empty, full, half),
 * 8 pixels apart like the hunger row, filling right to left. A flash draws every icon a second time
 * adding light, so empty icons flash too.
 */
final class SegmentHud {

    private static final int ICON = StaminaHudAssets.ICON;
    private static final int SPACING = 8;
    private static final int TEXT = 0xFFFFFFFF;
    private static final float FLASH_ALPHA = 0.6F;

    /**
     * @param right x of the row's right end (the rightmost icon ends here)
     * @param top y of the icons' top edge
     */
    void draw(Minecraft mc, int right, int top, float visible, float maximum, NumericText numericText,
            boolean flashing) {
        int halves = SegmentRow.halves(visible, maximum);
        mc.getTextureManager().bindTexture(StaminaHudAssets.SEGMENTS);
        GlStateManager.enableBlend();
        normalBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        icons(right, top, halves);
        if (flashing) {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, FLASH_ALPHA);
            icons(right, top, halves);
            normalBlend();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();

        String text = BarHud.text(visible, maximum, numericText);
        if (!text.isEmpty()) {
            mc.fontRenderer.drawStringWithShadow(text, right + 2, top + 1, TEXT);
        }

        // Vanilla draws the air bubbles right after this without resetting colour or texture.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(Gui.ICONS);
    }

    private static void normalBlend() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    private static void icons(int right, int top, int halves) {
        for (int segment = 0; segment < SegmentRow.SEGMENTS; segment++) {
            int x = right - ICON - segment * SPACING;
            Gui.drawModalRectWithCustomSizedTexture(x, top, u(SegmentRow.state(segment, halves)), 0, ICON, ICON,
                    StaminaHudAssets.ATLAS_WIDTH, ICON);
        }
    }

    /** Atlas column of a state: empty, full, half, left to right. */
    static int u(int state) {
        return state == SegmentRow.FULL ? StaminaHudAssets.FULL_U
                : state == SegmentRow.HALF ? StaminaHudAssets.HALF_U : StaminaHudAssets.EMPTY_U;
    }
}
