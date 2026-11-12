package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.BarPosition;
import com.mahghuuuls.jass.client.JassClientConfig.FillDirection;
import net.minecraftforge.common.config.Config.RangeInt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    void defaultSettingsGiveTheSameBarAsBefore() {
        BarLayout layout = BarLayout.compute(BarPosition.RIGHT_STACK, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(W / 2 + 91 - 81, layout.left);
        assertEquals(H - 49 + 2, layout.top);
        assertEquals(81, layout.width);
        assertEquals(5, layout.height);
        assertArrayEquals(new int[] {layout.left, layout.top, layout.left + 41, layout.top + 5},
                layout.fillForShare(FillDirection.LEFT_TO_RIGHT, 0.5F));
    }

    @Test
    void centerIsCentredRoundingDown() {
        BarLayout layout = BarLayout.compute(BarPosition.CENTER, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(173, layout.left);
        assertEquals(117, layout.top);
    }

    @Test
    void middleLeftTouchesTheLeftEdgeAndIsCentredVertically() {
        BarLayout layout = BarLayout.compute(BarPosition.MIDDLE_LEFT, 5, 60, 0, 0, W, H, 49, 49);
        assertEquals(3, layout.left);
        assertEquals(90, layout.top);
    }

    @Test
    void edgeCentresAndCornersSitThreePixelsFromTheirEdges() {
        assertPlaced(BarPosition.TOP_CENTER, 173, 3);
        assertPlaced(BarPosition.BOTTOM_CENTER, 173, H - 3 - 5);
        assertPlaced(BarPosition.MIDDLE_RIGHT, W - 3 - 81, 117);
        assertPlaced(BarPosition.TOP_LEFT, 3, 3);
        assertPlaced(BarPosition.BOTTOM_RIGHT, W - 3 - 81, H - 3 - 5);
    }

    private static void assertPlaced(BarPosition position, int left, int top) {
        BarLayout layout = BarLayout.compute(position, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(left, layout.left, position.name());
        assertEquals(top, layout.top, position.name());
    }

    @Test
    void numbersGoLeftOnlyForTheLeftStackAndTheRightEdgeAnchors() {
        for (BarPosition position : BarPosition.values()) {
            boolean expected = position == BarPosition.LEFT_STACK || position == BarPosition.TOP_RIGHT
                    || position == BarPosition.MIDDLE_RIGHT || position == BarPosition.BOTTOM_RIGHT;
            assertEquals(expected, BarLayout.compute(position, 81, 5, 0, 0, W, H, 49, 49).textOnLeft, position.name());
        }
    }

    @Test
    void verticalBarsFillFromTheirStartAndEmptyBackTowardIt() {
        BarLayout layout = BarLayout.compute(BarPosition.MIDDLE_LEFT, 5, 60, 0, 0, W, H, 49, 49);
        int l = layout.left;
        int t = layout.top;
        assertArrayEquals(new int[] {l, t + 30, l + 5, t + 60}, layout.fillForShare(FillDirection.BOTTOM_TO_TOP, 0.5F));
        assertArrayEquals(new int[] {l, t, l + 5, t + 30}, layout.fillForShare(FillDirection.TOP_TO_BOTTOM, 0.5F));
        assertArrayEquals(new int[] {l, t, l + 5, t + 60}, layout.fillForShare(FillDirection.BOTTOM_TO_TOP, 1.0F));
        assertArrayEquals(new int[] {l, t + 60, l + 5, t + 60}, layout.fillForShare(FillDirection.BOTTOM_TO_TOP, 0.0F));
        assertArrayEquals(new int[] {l, t, l + 5, t}, layout.fillForShare(FillDirection.TOP_TO_BOTTOM, 0.0F));
    }

    @Test
    void horizontalFillsStartAtTheirNamedEndAndShareIsClamped() {
        BarLayout layout = BarLayout.compute(BarPosition.CENTER, 81, 5, 0, 0, W, H, 49, 49);
        int l = layout.left;
        int t = layout.top;
        assertArrayEquals(new int[] {l + 81 - 41, t, l + 81, t + 5}, layout.fillForShare(FillDirection.RIGHT_TO_LEFT, 0.5F));
        assertArrayEquals(new int[] {l, t, l + 81, t + 5}, layout.fillForShare(FillDirection.LEFT_TO_RIGHT, 2.0F));
        assertArrayEquals(new int[] {l, t, l, t + 5}, layout.fillForShare(FillDirection.LEFT_TO_RIGHT, -1.0F));
    }

    @Test
    void anOffsetPastTheEdgeStopsWithTheBorderOnScreen() {
        BarLayout right = BarLayout.compute(BarPosition.TOP_LEFT, 81, 5, 1000, 0, W, H, 49, 49);
        assertEquals(W, right.left + 81 + 1);
        BarLayout left = BarLayout.compute(BarPosition.TOP_RIGHT, 81, 5, -1000, 0, W, H, 49, 49);
        assertEquals(1, left.left);
        BarLayout up = BarLayout.compute(BarPosition.BOTTOM_LEFT, 81, 5, 0, 1000, W, H, 49, 49);
        assertEquals(1, up.top);
        BarLayout down = BarLayout.compute(BarPosition.TOP_LEFT, 81, 5, 0, -1000, W, H, 49, 49);
        assertEquals(H, down.top + 5 + 1);
    }

    @Test
    void aBarLargerThanTheScreenStartsAtTheLeftAndTopEdge() {
        BarLayout layout = BarLayout.compute(BarPosition.CENTER, 400, 400, 0, 0, 320, 180, 49, 49);
        assertEquals(1, layout.left);
        assertEquals(1, layout.top);
    }

    @Test
    void everyPositionStaysOnScreenAtCommonGuiSizes() {
        int[][] screens = {{1280, 720}, {640, 360}, {427, 240}, {320, 180}};
        int[][] sizes = {{81, 5}, {5, 81}, {200, 20}, {1, 1}};
        for (int[] screen : screens) {
            for (int[] size : sizes) {
                for (BarPosition position : BarPosition.values()) {
                    for (int[] offset : new int[][] {{0, 0}, {5000, 5000}, {-5000, -5000}}) {
                        BarLayout layout = BarLayout.compute(position, size[0], size[1], offset[0], offset[1],
                                screen[0], screen[1], 49, 49);
                        String where = position + " " + size[0] + "x" + size[1] + " on " + screen[0] + "x" + screen[1];
                        assertTrue(layout.left - 1 >= 0 && layout.left + size[0] + 1 <= screen[0], where);
                        assertTrue(layout.top - 1 >= 0 && layout.top + size[1] + 1 <= screen[1], where);
                    }
                }
            }
        }
    }

    @Test
    void aTallBarInTheLeftStackTakesARowAsTallAsItNeeds() {
        BarLayout layout = BarLayout.compute(BarPosition.LEFT_STACK, 5, 40, 0, 0, W, H, 49, 49);
        assertEquals(49 + 45, layout.leftHeight);
        assertEquals(49, layout.rightHeight);
        assertEquals(H - 49 + 2 + 5, layout.top + layout.height);
    }

    @Test
    void anchoredBarsLeaveTheColumnHeightsAlone() {
        for (BarPosition position : BarPosition.values()) {
            if (position == BarPosition.RIGHT_STACK || position == BarPosition.LEFT_STACK) {
                continue;
            }
            BarLayout layout = BarLayout.compute(position, 81, 5, 0, 0, W, H, 49, 59);
            assertEquals(49, layout.rightHeight, position.name());
            assertEquals(59, layout.leftHeight, position.name());
        }
    }

    @Test
    void aClampedStackBarKeepsItsReservedRow() {
        BarLayout layout = BarLayout.compute(BarPosition.RIGHT_STACK, 81, 5, 5000, 0, W, H, 49, 49);
        assertEquals(W - 1 - 81, layout.left);
        assertEquals(59, layout.rightHeight);
    }

    @Test
    void numbersMoveToTheOtherSideOnlyWhenTheirSideIsOffScreen() {
        BarLayout middle = BarLayout.compute(BarPosition.CENTER, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(173 + 81 + 3, middle.textX(30));
        BarLayout atRightEdge = BarLayout.compute(BarPosition.TOP_LEFT, 81, 5, 1000, 0, W, H, 49, 49);
        assertEquals(345 - 3 - 30, atRightEdge.textX(30));
        BarLayout atLeftEdge = BarLayout.compute(BarPosition.LEFT_STACK, 81, 5, -1000, 0, W, H, 49, 49);
        assertEquals(1 + 81 + 3, atLeftEdge.textX(30));
        BarLayout rightAnchor = BarLayout.compute(BarPosition.TOP_RIGHT, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(W - 3 - 81 - 3 - 30, rightAnchor.textX(30));
        BarLayout full = BarLayout.compute(BarPosition.TOP_LEFT, 425, 5, 0, 0, W, H, 49, 49);
        assertEquals(1 + 425 + 3, full.textX(30));
    }

    @Test
    void numbersAreCentredOnTheBar() {
        BarLayout layout = BarLayout.compute(BarPosition.MIDDLE_LEFT, 5, 60, 0, 0, W, H, 49, 49);
        assertEquals(90 + (60 - 9) / 2, layout.textY());
    }

    @Test
    void theFillFollowsStaminaAndIsEmptyWithoutAMaximum() {
        BarLayout layout = BarLayout.compute(BarPosition.CENTER, 81, 5, 0, 0, W, H, 49, 49);
        assertArrayEquals(layout.fillForShare(FillDirection.LEFT_TO_RIGHT, 0.5F), layout.fill(FillDirection.LEFT_TO_RIGHT, 30.0F, 60.0F));
        assertArrayEquals(new int[] {173, 117, 173, 122}, layout.fill(FillDirection.LEFT_TO_RIGHT, 30.0F, 0.0F));
        assertArrayEquals(new int[] {173, 117, 254, 122}, layout.fillForShare(FillDirection.RIGHT_TO_LEFT, 1.0F));
        assertArrayEquals(new int[] {254, 117, 254, 122}, layout.fillForShare(FillDirection.RIGHT_TO_LEFT, 0.0F));
        BarLayout standing = BarLayout.compute(BarPosition.MIDDLE_RIGHT, 5, 60, 0, 0, W, H, 49, 49);
        assertArrayEquals(new int[] {419, 90, 424, 150}, standing.fillForShare(FillDirection.TOP_TO_BOTTOM, 1.0F));
    }

    @Test
    void largerThanTheScreenAlsoStartsAtTheEdgeForStacksAndCorners() {
        for (BarPosition position : new BarPosition[] {BarPosition.RIGHT_STACK, BarPosition.BOTTOM_RIGHT}) {
            BarLayout layout = BarLayout.compute(position, 400, 400, 0, 0, 320, 180, 49, 49);
            assertEquals(1, layout.left, position.name());
            assertEquals(1, layout.top, position.name());
        }
    }

    @Test
    void sizeSettingsAllowOneToFourHundred() throws NoSuchFieldException {
        for (String field : new String[] {"barWidth", "barHeight"}) {
            RangeInt range = JassClientConfig.class.getField(field).getAnnotation(RangeInt.class);
            assertEquals(1, range.min(), field);
            assertEquals(400, range.max(), field);
        }
    }

    @Test
    void leftSideNumbersStayLeftWhenTheyFitAndWhenNeitherSideFits() {
        BarLayout leftStack = BarLayout.compute(BarPosition.LEFT_STACK, 81, 5, 0, 0, W, H, 49, 49);
        assertEquals(122 - 3 - 30, leftStack.textX(30));
        BarLayout wide = BarLayout.compute(BarPosition.TOP_RIGHT, 425, 5, 0, 0, W, H, 49, 49);
        assertEquals(1 - 3 - 30, wide.textX(30));
    }

    @Test
    void numbersThatEndExactlyAtTheScreenEdgeStillFit() {
        BarLayout leftEdge = BarLayout.compute(BarPosition.LEFT_STACK, 81, 5, -89, 0, W, H, 49, 49);
        assertEquals(0, leftEdge.textX(30));
        BarLayout rightEdge = BarLayout.compute(BarPosition.TOP_LEFT, 81, 5, 310, 0, W, H, 49, 49);
        assertEquals(W - 30, rightEdge.textX(30));
    }
}
