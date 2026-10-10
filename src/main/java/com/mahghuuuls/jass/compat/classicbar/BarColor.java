package com.mahghuuuls.jass.compat.classicbar;

/** Parses {@code classicbar_bar_color}; plain arithmetic, so it is tested without Classic Bar. */
final class BarColor {

    /** The default {@code #E3B834}, used when the configured value is not a {@code #RRGGBB} color. */
    static final int DEFAULT = 0xE3B834;

    private BarColor() {
    }

    /** The RGB value of {@code #RRGGBB} (case-insensitive, surrounding spaces ignored), or -1. */
    static int parse(String value) {
        if (value == null) {
            return -1;
        }
        String trimmed = value.trim();
        if (trimmed.length() != 7 || trimmed.charAt(0) != '#') {
            return -1;
        }
        int rgb = 0;
        for (int i = 1; i < 7; i++) {
            int digit = Character.digit(trimmed.charAt(i), 16);
            if (digit < 0) {
                return -1;
            }
            rgb = rgb * 16 + digit;
        }
        return rgb;
    }
}
