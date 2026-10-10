package com.mahghuuuls.jass.compat.classicbar;

import com.mahghuuuls.jass.client.ClientStaminaState;
import com.mahghuuuls.jass.client.HudPresenter;
import com.mahghuuuls.jass.client.JassClientConfig;
import com.mahghuuuls.jass.client.StaminaHudAssets;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.Logger;
import tfar.classicbar.Color;
import tfar.classicbar.ModUtils;
import tfar.classicbar.config.ModConfig;
import tfar.classicbar.overlays.IBarOverlay;

/**
 * The {@code jassstamina} Classic Bar Legacy bar (REQ-054), following the owner's
 * {@code jawms-classic-bar} pattern: Classic Bar's own bar texture (already bound for the bar pass),
 * geometry, and helpers, so it matches the built-in bars. Values, visibility, and the denial flash
 * come from the presenter, so they match the built-in displays. The fill hugs the outer edge of
 * whichever column the player puts it in, like Classic Bar's own bars.
 */
public final class StaminaBarOverlay implements IBarOverlay {

    public static final String NAME = "jassstamina";

    private static final int RIGHT_CLUSTER = 10;
    private static final int LEFT_CLUSTER = -91;
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 9;
    /** Classic Bar caps the fill at 78 of the 79 inner pixels. */
    private static final int FILL_MAX = 78;
    private static final int FILL_HEIGHT = 7;
    private static final int FILL_U = 1;
    private static final int FILL_V = 10;
    private static final int ICON = 9;
    private static final int FLASH = 0xFFF2F2F2;

    private final HudPresenter presenter;
    private final Logger logger;
    private boolean right;
    private boolean settingsFailed;
    private String colorSource;
    private int color = BarColor.DEFAULT;

    public StaminaBarOverlay(HudPresenter presenter, Logger logger) {
        this.presenter = presenter;
        this.logger = logger;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public IBarOverlay setSide(boolean side) {
        right = side;
        return this;
    }

    @Override
    public boolean rightHandSide() {
        return right;
    }

    /** Cheap: Classic Bar asks every bar up to three times per frame. Hidden bars take no row. */
    @Override
    public boolean shouldRender(EntityPlayer player) {
        return presenter.shown();
    }

    @Override
    public void renderBar(EntityPlayer player, int width, int height) {
        int left = barLeft(width);
        int top = height - getSidedOffset();
        int fill = ModUtils.getWidth(ClientStaminaState.visible(), ClientStaminaState.maximum());
        int rgb = color();
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        try {
            Color.reset();
            ModUtils.drawTexturedModalRect(left, top, 0, 0, BAR_WIDTH, BAR_HEIGHT);
            if (fill > 0) {
                GlStateManager.color(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
                int fillLeft = right ? left + FILL_MAX + 1 - fill : left + 1;
                ModUtils.drawTexturedModalRect(fillLeft, top + 1, FILL_U, FILL_V, fill, FILL_HEIGHT);
            }
            if (presenter.flashing()) {
                // A light frame, visible even when the bar is empty (most denials happen at 0).
                Gui.drawRect(left, top, left + BAR_WIDTH, top + 1, FLASH);
                Gui.drawRect(left, top + BAR_HEIGHT - 1, left + BAR_WIDTH, top + BAR_HEIGHT, FLASH);
                Gui.drawRect(left, top, left + 1, top + BAR_HEIGHT, FLASH);
                Gui.drawRect(left + BAR_WIDTH - 1, top, left + BAR_WIDTH, top + BAR_HEIGHT, FLASH);
                GlStateManager.enableBlend();
            }
        } finally {
            Color.reset();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldRenderText() {
        return JassClientConfig.classicbarShowNumbers;
    }

    /** Visible Stamina, or its percentage when Classic Bar's own percentage setting is on. */
    @Override
    public void renderText(EntityPlayer player, int width, int height) {
        float visible = ClientStaminaState.visible();
        float maximum = ClientStaminaState.maximum();
        int value = showPercent()
                ? (maximum > 0.0F ? (int) (100.0F * visible / maximum) : 0)
                : (int) Math.floor(visible);
        String text = Integer.toString(value);
        int left = barLeft(width);
        int iconGap = showIcons() ? ICON : 0;
        int textLeft = right ? left + iconGap + ModUtils.rightTextOffset
                : left - iconGap - ModUtils.getStringLength(text) + ModUtils.leftTextOffset;
        try {
            ModUtils.drawStringOnHUD(text, textLeft, height - getSidedOffset() - 1, color());
        } finally {
            Color.reset();
        }
    }

    /** The full Stamina segment icon beside the bar, when Classic Bar draws icons. */
    @Override
    public void renderIcon(EntityPlayer player, int width, int height) {
        int left = barLeft(width);
        int iconLeft = right ? left + BAR_WIDTH + 1 : left - ICON - 1;
        Color.reset();
        ModUtils.mc.getTextureManager().bindTexture(StaminaHudAssets.SEGMENTS);
        try {
            GlStateManager.enableBlend();
            Gui.drawModalRectWithCustomSizedTexture(iconLeft, height - getSidedOffset(), StaminaHudAssets.FULL_U, 0,
                    ICON, ICON, StaminaHudAssets.ATLAS_WIDTH, ICON);
            GlStateManager.disableBlend();
        } finally {
            // The vanilla icon sheet is what the next bar's icon expects (Classic Bar's resting state).
            ModUtils.mc.getTextureManager().bindTexture(Gui.ICONS);
        }
    }

    /** Classic Bar's own percentage setting; false after one warning if it cannot be read (ERR-1). */
    private boolean showPercent() {
        if (!settingsFailed) {
            try {
                return ModConfig.numbers.showPercent;
            } catch (LinkageError | RuntimeException e) {
                settingsFailed(e);
            }
        }
        return false;
    }

    /** Classic Bar's own icon setting (icons are drawn only when Classic Bar calls renderIcon). */
    private boolean showIcons() {
        if (!settingsFailed) {
            try {
                return ModConfig.general.displayIcons;
            } catch (LinkageError | RuntimeException e) {
                settingsFailed(e);
            }
        }
        return false;
    }

    private void settingsFailed(Throwable e) {
        settingsFailed = true;
        logger.warn("Could not read Classic Bar Legacy's number settings ({}); showing plain Stamina numbers", e.toString());
    }

    private int barLeft(int width) {
        return width / 2 + (right ? RIGHT_CLUSTER : LEFT_CLUSTER);
    }

    /** The configured color, re-read when the setting changes; an invalid value warns once per value. */
    private int color() {
        String source = JassClientConfig.classicbarBarColor;
        if (source == null ? colorSource != null : !source.equals(colorSource)) {
            colorSource = source;
            int parsed = BarColor.parse(source);
            if (parsed < 0) {
                logger.warn("classicbar_bar_color \"{}\" is not a #RRGGBB color; using #E3B834", source);
                parsed = BarColor.DEFAULT;
            }
            color = parsed;
        }
        return color;
    }
}
