package com.mahghuuuls.jass.api;

import net.minecraft.util.ResourceLocation;

/** Identifiers of the built-in Stamina actions and causes. Addons use their own namespace. */
public final class JassActions {

    public static final ResourceLocation MELEE = new ResourceLocation("jass", "melee");
    public static final ResourceLocation SPRINT = new ResourceLocation("jass", "sprint");
    public static final ResourceLocation BOW = new ResourceLocation("jass", "bow");
    public static final ResourceLocation JUMP = new ResourceLocation("jass", "jump");
    public static final ResourceLocation BLOCK = new ResourceLocation("jass", "block");
    /** Cause of {@code /stamina set} and {@code /stamina restore}. */
    public static final ResourceLocation COMMAND = new ResourceLocation("jass", "command");

    private JassActions() {
    }
}
