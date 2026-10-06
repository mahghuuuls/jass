package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.NumericText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;

/** Draws the Stamina bar in the right-hand bar column above the hotbar. */
final class BarHud {

    private static final int WIDTH = 81;
    private static final int HEIGHT = 5;
    private static final int ROW = 10;
    private static final int BORDER = 0xFF1A1A1A;
    private static final int FLASH_BORDER = 0xFFF2F2F2;
    private static final int EMPTY = 0xFF3A3326;
    private static final int FILL = 0xFFE3B834;
    private static final int TEXT = 0xFFFFFFFF;

    void draw(Minecraft mc, ScaledResolution resolution, float visible, float maximum, NumericText numericText,
            boolean flashing) {
        int left = resolution.getScaledWidth() / 2 + 10;
        int top = resolution.getScaledHeight() - GuiIngameForge.right_height + 2;
        GuiIngameForge.right_height += ROW;

        Gui.drawRect(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, flashing ? FLASH_BORDER : BORDER);
        Gui.drawRect(left, top, left + WIDTH, top + HEIGHT, EMPTY);
        int filled = Math.round(WIDTH * Math.min(1.0F, visible / maximum));
        if (filled > 0) {
            Gui.drawRect(left, top, left + filled, top + HEIGHT, FILL);
        }

        String text = text(visible, maximum, numericText);
        if (!text.isEmpty()) {
            mc.fontRenderer.drawStringWithShadow(text, left + WIDTH + 3, top - 2, TEXT);
        }

        // Vanilla draws the air bubbles right after this without resetting colour or texture.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(Gui.ICONS);
    }

    private static String text(float visible, float maximum, NumericText mode) {
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
