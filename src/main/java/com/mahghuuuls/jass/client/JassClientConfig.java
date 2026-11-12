package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.Tags;
import net.minecraftforge.common.config.Config;

/** Per-player display settings, written to {@code config/jass/jass-client.cfg}. */
@Config(modid = Tags.MOD_ID, name = "jass/jass-client", category = "hud")
@Config.LangKey("config.jass.client")
public final class JassClientConfig {

    private JassClientConfig() {
    }

    public enum NumericText {
        HIDDEN,
        CURRENT,
        MAXIMUM,
        CURRENT_AND_MAXIMUM
    }

    /**
     * Where the bar style sits: in a bar column above the hotbar, or anchored to one of nine screen
     * points (column 0, 1 or 2 from left to right, row 0, 1 or 2 from top to bottom).
     */
    public enum BarPosition {
        RIGHT_STACK(-1, -1),
        LEFT_STACK(-1, -1),
        TOP_LEFT(0, 0),
        TOP_CENTER(1, 0),
        TOP_RIGHT(2, 0),
        MIDDLE_LEFT(0, 1),
        CENTER(1, 1),
        MIDDLE_RIGHT(2, 1),
        BOTTOM_LEFT(0, 2),
        BOTTOM_CENTER(1, 2),
        BOTTOM_RIGHT(2, 2);

        /** Horizontal anchor 0, 1 or 2; -1 for a stack position. */
        final int column;
        /** Vertical anchor 0, 1 or 2; -1 for a stack position. */
        final int row;

        BarPosition(int column, int row) {
            this.column = column;
            this.row = row;
        }

        boolean isStack() {
            return column < 0;
        }
    }

    public enum HudStyle {
        BAR,
        SEGMENTS
    }

    public enum ItemTooltips {
        ALWAYS,
        SHIFT,
        OFF
    }

    /** Where the fill starts; it shrinks back toward that end as Stamina falls. */
    public enum FillDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        BOTTOM_TO_TOP,
        TOP_TO_BOTTOM
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

    @Config.Name("hide_elenai_weight_tooltip")
    @Config.Comment("With Elenai Dodge 2: hide its armor weight tooltip (steel feathers) while the server takes armor weight from Elenai; JASS's own Weight line shows the same weight.")
    public static boolean hideElenaiWeightTooltip = true;

    @Config.Name("denied_action_sound")
    @Config.Comment("Play a soft sound, at most once per second, when an action is refused for lack of Stamina.")
    public static boolean deniedActionSound = false;

    @Config.Name("hud_numeric_text")
    @Config.Comment("Numbers shown next to the Stamina display.")
    public static NumericText hudNumericText = NumericText.HIDDEN;

    @Config.Name("bar_position")
    @Config.Comment({"Where the Stamina bar sits. RIGHT_STACK and LEFT_STACK place it in the bar column above the hotbar,",
            "stacked with the other bars (and after Classic Bar's bars). The other values anchor it to that point of the screen,",
            "3 pixels from the screen edges it touches. BOTTOM_CENTER covers the hotbar (and, when lifted with bar_y_offset, the experience bar just above it).",
            "The bar always stays on screen."})
    public static BarPosition barPosition = BarPosition.RIGHT_STACK;

    @Config.Name("bar_width")
    @Config.Comment({"Width of the Stamina bar's fill area on screen, in GUI pixels. In a stack position, widths above 91 reach past the",
            "screen centre into the other column. For a bar filling BOTTOM_TO_TOP or TOP_TO_BOTTOM this is its thickness."})
    @Config.RangeInt(min = 1, max = 400)
    public static int barWidth = 81;

    @Config.Name("bar_height")
    @Config.Comment({"Height of the Stamina bar's fill area on screen, in GUI pixels. Taller bars take a taller row in the bar column.",
            "For a bar filling BOTTOM_TO_TOP or TOP_TO_BOTTOM this is its length."})
    @Config.RangeInt(min = 1, max = 400)
    public static int barHeight = 5;

    @Config.Name("bar_x_offset")
    @Config.Comment("Moves the Stamina bar sideways, in GUI pixels. Positive moves right. Other bars keep their places.")
    public static int barXOffset = 0;

    @Config.Name("bar_y_offset")
    @Config.Comment("Moves the Stamina bar up or down, in GUI pixels. Positive moves up. Other bars keep their places.")
    public static int barYOffset = 0;

    @Config.Name("bar_fill_direction")
    @Config.Comment({"Which end of the Stamina bar the fill starts from; it empties back toward that end.",
            "BOTTOM_TO_TOP and TOP_TO_BOTTOM fill along bar_height, so a standing bar also needs a small bar_width and a large bar_height (for example 5 and 60)."})
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

    @Config.Name("item_tooltips")
    @Config.Comment({"Stamina section in item tooltips: swing cost, bow drain, shield Stability and block shares,",
            "armor and held weight, and configured item Stamina modifiers, with the server's values.",
            "always, shift (only while Shift is held), or off."})
    public static ItemTooltips itemTooltips = ItemTooltips.ALWAYS;
}
