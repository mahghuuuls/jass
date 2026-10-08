package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.player.EntityPlayer;

/**
 * An integration that can suspend Stamina costs for a player (ARC-008), such as Inhibited outside
 * its effect. While suspended, no action costs Stamina and none is denied for Stamina; regeneration
 * and debt are unaffected. The default never suspends.
 */
public interface GatingSource {

    GatingSource NONE = player -> false;

    /** True while costs are suspended for this player. */
    boolean suspended(EntityPlayer player);
}
