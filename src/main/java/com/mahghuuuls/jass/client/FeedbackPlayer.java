package com.mahghuuuls.jass.client;

import net.minecraft.client.Minecraft;
import net.minecraft.init.SoundEvents;

/**
 * Player feedback for refused actions: a short flash of the Stamina display, and a quiet sound
 * at most once per second when enabled.
 */
final class FeedbackPlayer {

    private static final long FLASH_MILLIS = 500L;
    private static final long SOUND_INTERVAL_MILLIS = 1000L;
    private static final float SOUND_VOLUME = 0.4F;
    private static final float SOUND_PITCH = 0.7F;

    private long flashUntil;
    private long lastSound = Long.MIN_VALUE / 2;

    void onDenied() {
        long now = Minecraft.getSystemTime();
        flashUntil = now + FLASH_MILLIS;
        Minecraft mc = Minecraft.getMinecraft();
        if (JassClientConfig.deniedActionSound && mc.player != null && now - lastSound >= SOUND_INTERVAL_MILLIS) {
            lastSound = now;
            mc.player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, SOUND_VOLUME, SOUND_PITCH);
        }
    }

    /** True while the display should flash; blinks on and off during the flash. */
    boolean flashing() {
        long left = flashUntil - Minecraft.getSystemTime();
        return left > 0 && (left / 100) % 2 == 0;
    }

    /** True for the whole flash period, so a hidden display shows itself to flash. */
    boolean flashActive() {
        return flashUntil > Minecraft.getSystemTime();
    }
}
