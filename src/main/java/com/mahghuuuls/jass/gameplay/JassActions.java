package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.Tags;
import net.minecraft.util.ResourceLocation;

/** Identifiers of the built-in Stamina actions. */
public final class JassActions {

    public static final ResourceLocation MELEE = new ResourceLocation(Tags.MOD_ID, "melee");
    public static final ResourceLocation SPRINT = new ResourceLocation(Tags.MOD_ID, "sprint");
    public static final ResourceLocation BOW = new ResourceLocation(Tags.MOD_ID, "bow");
    public static final ResourceLocation JUMP = new ResourceLocation(Tags.MOD_ID, "jump");
    public static final ResourceLocation BLOCK = new ResourceLocation(Tags.MOD_ID, "block");

    private JassActions() {
    }
}
