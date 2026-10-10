package com.mahghuuuls.jass.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SegmentRowTest {

    /** States from the rightmost segment to the leftmost: F full, H half, E empty. */
    private static void assertRow(String expectedFromRight, float visible, float maximum) {
        int halves = SegmentRow.halves(visible, maximum);
        StringBuilder actual = new StringBuilder();
        for (int segment = 0; segment < SegmentRow.SEGMENTS; segment++) {
            int state = SegmentRow.state(segment, halves);
            actual.append(state == SegmentRow.FULL ? 'F' : state == SegmentRow.HALF ? 'H' : 'E');
        }
        assertEquals(expectedFromRight, actual.toString(), visible + " of " + maximum);
    }

    @Test
    void halfStaminaFillsFiveSegmentsFromTheRight() {
        assertRow("FFFFFEEEEE", 30, 60);
    }

    @Test
    void aValueHalfwayThroughASegmentShowsHalf() {
        assertRow("FFFFFHEEEE", 33, 60);
    }

    @Test
    void debtShowsAllEmpty() {
        assertRow("EEEEEEEEEE", -5, 60);
    }

    @Test
    void fullAndNearlyEmptyEnds() {
        assertRow("FFFFFFFFFF", 60, 60);
        assertRow("EEEEEEEEEE", 0, 60);
        assertRow("FFFFFFFFFH", 59, 60);
        assertRow("HEEEEEEEEE", 3, 60);
        assertRow("EEEEEEEEEE", 2.9F, 60);
    }

    @Test
    void noMaximumShowsAllEmpty() {
        assertRow("EEEEEEEEEE", 10, 0);
    }

    @Test
    void atlasColumnsAreEmptyFullHalf() {
        assertEquals(0, SegmentHud.u(SegmentRow.EMPTY));
        assertEquals(9, SegmentHud.u(SegmentRow.FULL));
        assertEquals(18, SegmentHud.u(SegmentRow.HALF));
    }
}
