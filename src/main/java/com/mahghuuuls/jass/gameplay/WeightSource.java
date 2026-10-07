package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.player.EntityPlayer;

/**
 * One way to find a player's Effective Weight. The standalone source always answers; an
 * integration source may decline, and then {@link WeightResolver} uses the standalone one.
 */
public interface WeightSource {

    /** Short name shown by {@code /stamina inspect}, such as {@code standalone}. */
    String id();

    /** Effective Weight of the player's worn armor, or {@code NaN} when this source cannot tell reliably. */
    double effectiveWeight(EntityPlayer player);
}
