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

    /** Where the bar style sits: in a bar column above the hotbar, or in a screen corner. */
    public enum BarPosition {
        RIGHT_STACK,
        LEFT_STACK,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public enum HudStyle {
        BAR,
        SEGMENTS
    }

    public enum FillDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT
    }

    @Config.Name("hud_hide_when_full")
    @Config.Comment("Hide the Stamina display while Stamina is full.")
    public static boolean hudHideWhenFull = true;

    @Config.Name("hud_hide_outside_inhibited")
    @Config.Comment("With Inhibited gating active: hide the Stamina display while the player does not have the Inhibited effect.")
    public static boolean hudHideOutsideInhibited = true;

    @Config.Name("hide_elenai_feather_hud")
    @Config.Comment("With Elenai Dodge 2: hide its feather bar while the server makes dodges cost Stamina instead of feathers.")
    public static boolean hideElenaiFeatherHud = true;

    @Config.Name("denied_action_sound")
    @Config.Comment("Play a soft sound, at most once per second, when an action is refused for lack of Stamina.")
    public static boolean deniedActionSound = false;

    @Config.Name("hud_numeric_text")
    @Config.Comment("Numbers shown next to the Stamina display.")
    public static NumericText hudNumericText = NumericText.HIDDEN;

    @Config.Name("bar_position")
    @Config.Comment({"Where the Stamina bar sits. right_stack and left_stack place it in the bar column above the hotbar,",
            "stacked with the other bars (and after Classic Bar's bars); the corner values place it at that screen corner."})
    public static BarPosition barPosition = BarPosition.RIGHT_STACK;

    @Config.Name("bar_width")
    @Config.Comment("Width of the Stamina bar's fill area, in GUI pixels. In a stack position, widths above 91 reach past the screen centre into the other column.")
    @Config.RangeInt(min = 10, max = 400)
    public static int barWidth = 81;

    @Config.Name("bar_height")
    @Config.Comment("Height of the Stamina bar's fill area, in GUI pixels. Taller bars take a taller row in the bar column.")
    @Config.RangeInt(min = 1, max = 20)
    public static int barHeight = 5;

    @Config.Name("bar_x_offset")
    @Config.Comment("Moves the Stamina bar sideways, in GUI pixels. Positive moves right. Other bars keep their places.")
    public static int barXOffset = 0;

    @Config.Name("bar_y_offset")
    @Config.Comment("Moves the Stamina bar up or down, in GUI pixels. Positive moves up. Other bars keep their places.")
    public static int barYOffset = 0;

    @Config.Name("bar_fill_direction")
    @Config.Comment("Which end of the Stamina bar the fill starts from.")
    public static FillDirection barFillDirection = FillDirection.LEFT_TO_RIGHT;

    @Config.Name("hud_style")
    @Config.Comment({"Built-in Stamina display: bar, or segments (a row of ten icons above the hunger row, like hunger).",
            "Not used while Classic Bar Legacy draws the Stamina bar."})
    public static HudStyle hudStyle = HudStyle.BAR;

    @Config.Name("segments_x_offset")
    @Config.Comment("Moves the segment row sideways, in GUI pixels. Positive moves right. Other bars keep their places.")
    public static int segmentsXOffset = 0;

    @Config.Name("segments_y_offset")
    @Config.Comment("Moves the segment row up or down, in GUI pixels. Positive moves up. Other bars keep their places.")
    public static int segmentsYOffset = 0;

    @Config.Name("classicbar_integration")
    @Config.Comment({"With Classic Bar Legacy: register the jassstamina bar. Add jassstamina to a Classic Bar bar order",
            "to show Stamina as a Classic Bar bar; while it is in an order, JASS draws no display of its own.",
            "Takes effect only when Classic Bar Legacy is installed. Needs a game restart."})
    @Config.RequiresMcRestart
    public static boolean classicbarIntegration = true;

    @Config.Name("classicbar_bar_color")
    @Config.Comment("Color of the jassstamina Classic Bar bar and its numbers, as #RRGGBB. An invalid value uses #E3B834 and logs a warning.")
    public static String classicbarBarColor = "#E3B834";

    @Config.Name("classicbar_show_numbers")
    @Config.Comment("Show the Stamina number beside the jassstamina Classic Bar bar (Classic Bar's percentage setting applies).")
    public static boolean classicbarShowNumbers = true;
}
