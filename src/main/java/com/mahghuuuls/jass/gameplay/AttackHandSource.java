package com.mahghuuuls.jass.gameplay;

import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Tells which hand performed an attack. Vanilla attacks always use the main hand; a combat mod
 * with off-hand attacks can supply its own source.
 */
public interface AttackHandSource {

    AttackHandSource MAIN_HAND = event -> EnumHand.MAIN_HAND;

    EnumHand handOf(AttackEntityEvent event);
}
