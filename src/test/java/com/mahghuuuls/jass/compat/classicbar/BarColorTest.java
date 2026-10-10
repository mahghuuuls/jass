package com.mahghuuuls.jass.compat.classicbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarColorTest {

    @Test
    void parsesRrggbb() {
        assertEquals(0xE3B834, BarColor.parse("#E3B834"));
        assertEquals(0xE3B834, BarColor.parse(" #e3b834 "));
        assertEquals(0x000000, BarColor.parse("#000000"));
    }

    @Test
    void rejectsAnythingElse() {
        assertEquals(-1, BarColor.parse("#zzzzzz"));
        assertEquals(-1, BarColor.parse("E3B834"));
        assertEquals(-1, BarColor.parse("#E3B8"));
        assertEquals(-1, BarColor.parse("#E3B83411"));
        assertEquals(-1, BarColor.parse(""));
        assertEquals(-1, BarColor.parse(null));
    }
}
