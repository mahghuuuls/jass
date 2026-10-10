package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.Tags;
import net.minecraft.util.ResourceLocation;

/** The Stamina segment atlas (IMP-018): three 9x9 regions, empty, full, half, left to right. */
public final class StaminaHudAssets {

    public static final ResourceLocation SEGMENTS = new ResourceLocation(Tags.MOD_ID, "textures/gui/stamina_segments.png");
    public static final int ICON = 9;
    public static final int ATLAS_WIDTH = 27;
    public static final int EMPTY_U = 0;
    public static final int FULL_U = 9;
    public static final int HALF_U = 18;

    private StaminaHudAssets() {
    }
}
