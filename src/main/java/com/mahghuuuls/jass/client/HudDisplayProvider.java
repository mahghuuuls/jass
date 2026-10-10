package com.mahghuuuls.jass.client;

/**
 * What an optional HUD mod tells the presenter (ARC-013). The client reaches compat code only
 * through this interface.
 */
public interface HudDisplayProvider {

    /** No other HUD mod. */
    HudDisplayProvider NONE = () -> false;

    /** True while the other mod draws the Stamina display itself, so no built-in display draws. */
    boolean drawsStamina();
}
