package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.core.StaminaProfile;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Resolves a player's Stamina Profile. Every stat read goes through here, so equipment and
 * addon modifiers can be added later without touching callers.
 */
public final class ProfileService {

    StaminaProfile profile(EntityPlayer player) {
        ServerSettings settings = ConfigModel.server();
        return new StaminaProfile(
                settings.maxStamina(),
                settings.staminaRegeneration(),
                settings.regenerationDelay(),
                0.0);
    }
}
