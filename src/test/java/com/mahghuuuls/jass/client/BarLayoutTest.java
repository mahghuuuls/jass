package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.BarPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarLayoutTest {

    private static final int W = 427;
    private static final int H = 240;

    @Test
    void defaultsSitInTheRowAboveTheFoodRow() {
        // Vanilla: right_height is 49 after the food row.
        BarLayout layout = BarLayout.compute(BarPosition.RIGHT_STACK, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(W / 2 + 10, layout.left);
        assertEquals(H - 49 + 2, layout.top);
        assertEquals(59, layout.rightHeight);
        assertEquals(49, layout.leftHeight);
        assertFalse(layout.textOnLeft);
    }

    @Test
    void stacksAfterClassicBarBars() {
        // Classic Bar replaced the food row and drew three right bars, advancing right_height 39 -> 69.
        BarLayout layout = BarLayout.compute(BarPosition.RIGHT_STACK, 81, 5, 0, 0, W, H, 69, 39);
        assertEquals(H - 69 + 2, layout.top);
        assertEquals(79, layout.rightHeight);
    }

    @Test
    void segmentRowEndsAtTheHotbarEdgeInTheNextRow() {
        int[] row = BarLayout.segments(0, 0, W, H, 49);
        assertEquals(W / 2 + 91, row[0]);
        assertEquals(H - 49, row[1]);
        assertEquals(59, row[2]);
        int[] moved = BarLayout.segments(0, -10, W, H, 49);
        assertEquals(H - 49 + 10, moved[1]);
        assertEquals(59, moved[2]);
    }

    @Test
    void offsetsMoveOnlyTheBar() {
        BarLayout layout = BarLayout.compute(BarPosition.RIGHT_STACK, 81, 5, 20, 10, W, H, 49, 49);
        assertEquals(W / 2 + 10 + 20, layout.left);
        assertEquals(H - 49 + 2 - 10, layout.top);
        assertEquals(59, layout.rightHeight);
    }

    @Test
    void tallBarsTakeATallerRowAndKeepTheirRightEdge() {
        BarLayout layout = BarLayout.compute(BarPosition.RIGHT_STACK, 60, 9, 0, 0, W, H, 49, 49);
        assertEquals(W / 2 + 91 - 60, layout.left);
        assertEquals(63, layout.rightHeight);
        // The bar's bottom edge stays where the default bar's bottom edge is.
        assertEquals(H - 49 + 2 + 5, layout.top + layout.height);
    }

    @Test
    void leftStackUsesTheLeftColumn() {
        BarLayout layout = BarLayout.compute(BarPosition.LEFT_STACK, 81, 5, 0, 0, W, H, 49, 69);
        assertEquals(W / 2 - 91, layout.left);
        assertEquals(H - 69 + 2, layout.top);
        assertEquals(79, layout.leftHeight);
        assertEquals(49, layout.rightHeight);
        assertTrue(layout.textOnLeft);
    }

    @Test
    void cornersLeaveTheColumnsAlone() {
        BarLayout topRight = BarLayout.compute(BarPosition.TOP_RIGHT, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(W - 3 - 81, topRight.left);
        assertEquals(3, topRight.top);
        assertEquals(49, topRight.rightHeight);
        assertTrue(topRight.textOnLeft);
        BarLayout bottomLeft = BarLayout.compute(BarPosition.BOTTOM_LEFT, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(3, bottomLeft.left);
        assertEquals(H - 3 - 5, bottomLeft.top);
        assertFalse(bottomLeft.textOnLeft);
    }
}
