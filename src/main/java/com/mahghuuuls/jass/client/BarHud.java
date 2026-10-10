package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.FillDirection;
import com.mahghuuuls.jass.client.JassClientConfig.NumericText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/** Draws the Stamina bar where the presenter's layout puts it. */
final class BarHud {

    private static final int BORDER = 0xFF1A1A1A;
    private static final int FLASH_BORDER = 0xFFF2F2F2;
    private static final int EMPTY = 0xFF3A3326;
    private static final int FILL = 0xFFE3B834;
    private static final int TEXT = 0xFFFFFFFF;

    void draw(Minecraft mc, BarLayout layout, float visible, float maximum, NumericText numericText,
            FillDirection direction, boolean flashing) {
        int left = layout.left;
        int top = layout.top;
        int right = left + layout.width;
        int bottom = top + layout.height;

        Gui.drawRect(left - 1, top - 1, right + 1, bottom + 1, flashing ? FLASH_BORDER : BORDER);
        Gui.drawRect(left, top, right, bottom, EMPTY);
        int filled = Math.round(layout.width * Math.min(1.0F, visible / maximum));
        if (filled > 0) {
            if (direction == FillDirection.RIGHT_TO_LEFT) {
                Gui.drawRect(right - filled, top, right, bottom, FILL);
            } else {
                Gui.drawRect(left, top, left + filled, bottom, FILL);
            }
        }

        String text = text(visible, maximum, numericText);
        if (!text.isEmpty()) {
            int textX = layout.textOnLeft ? left - 3 - mc.fontRenderer.getStringWidth(text) : right + 3;
            mc.fontRenderer.drawStringWithShadow(text, textX, top + (layout.height - 9) / 2, TEXT);
        }

        // Vanilla draws the air bubbles right after this without resetting colour or texture.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(Gui.ICONS);
    }

    static String text(float visible, float maximum, NumericText mode) {
        int current = (int) Math.floor(visible);
        int max = (int) Math.floor(maximum);
        switch (mode) {
            case CURRENT:
                return Integer.toString(current);
            case MAXIMUM:
                return Integer.toString(max);
            case CURRENT_AND_MAXIMUM:
                return current + "/" + max;
            default:
                return "";
        }
    }
}
