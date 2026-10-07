package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.Tags;
import net.minecraftforge.common.config.Config;

/** Per-player display settings, written to {@code config/jass/jass-client.cfg}. */
@Config(modid = Tags.MOD_ID, name = "jass/jass-client", category = "hud")
public final class JassClientConfig {

    private JassClientConfig() {
    }

    public enum NumericText {
        HIDDEN,
        CURRENT,
        MAXIMUM,
        CURRENT_AND_MAXIMUM
    }

    @Config.Name("hud_hide_when_full")
    @Config.Comment("Hide the Stamina display while Stamina is full.")
    public static boolean hudHideWhenFull = true;

    @Config.Name("denied_action_sound")
    @Config.Comment("Play a soft sound, at most once per second, when an action is refused for lack of Stamina.")
    public static boolean deniedActionSound = false;

    @Config.Name("hud_numeric_text")
    @Config.Comment("Numbers shown next to the Stamina display.")
    public static NumericText hudNumericText = NumericText.HIDDEN;
}
