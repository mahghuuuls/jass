package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.BarPosition;

/**
 * Where the bar style draws this frame, as plain arithmetic so it can be tested without a game.
 *
 * <p>Stack positions take one row of the vanilla bar column: rows are 10 pixels like vanilla's,
 * or taller for a tall bar, and start above the column's current height (which already includes
 * Classic Bar's bars: Classic Bar advances Forge's heights for each bar it draws). Offsets move only
 * the bar; the reserved row stays where it was, so other bars keep their places.
 */
final class BarLayout {

    /** Gap between a corner position and the screen edge, outside the bar's 1-pixel border. */
    private static final int CORNER_MARGIN = 3;
    private static final int VANILLA_ROW = 10;
    /** Half the hotbar width: the bar columns end at the screen centre plus or minus this. */
    private static final int HALF_HOTBAR = 91;

    final int left;
    final int top;
    final int width;
    final int height;
    /** True when the numbers go on the bar's left, away from the screen edge or the hotbar. */
    final boolean textOnLeft;
    /** Forge's right and left column heights after this bar (unchanged for corner positions). */
    final int rightHeight;
    final int leftHeight;

    private BarLayout(int left, int top, int width, int height, boolean textOnLeft, int rightHeight, int leftHeight) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.textOnLeft = textOnLeft;
        this.rightHeight = rightHeight;
        this.leftHeight = leftHeight;
    }

    /** @param rightHeight Forge's {@code right_height} now (likewise {@code leftHeight}) */
    static BarLayout compute(BarPosition position, int width, int height, int xOffset, int yOffset,
            int screenWidth, int screenHeight, int rightHeight, int leftHeight) {
        int row = Math.max(VANILLA_ROW, height + 5);
        int left;
        int top;
        boolean textOnLeft;
        int newRight = rightHeight;
        int newLeft = leftHeight;
        switch (position) {
            case LEFT_STACK:
                top = rowTop(screenHeight, leftHeight, row) + row - height - 3;
                left = screenWidth / 2 - HALF_HOTBAR;
                textOnLeft = true;
                newLeft = leftHeight + row;
                break;
            case TOP_LEFT:
                left = CORNER_MARGIN;
                top = CORNER_MARGIN;
                textOnLeft = false;
                break;
            case TOP_RIGHT:
                left = screenWidth - CORNER_MARGIN - width;
                top = CORNER_MARGIN;
                textOnLeft = true;
                break;
            case BOTTOM_LEFT:
                left = CORNER_MARGIN;
                top = screenHeight - CORNER_MARGIN - height;
                textOnLeft = false;
                break;
            case BOTTOM_RIGHT:
                left = screenWidth - CORNER_MARGIN - width;
                top = screenHeight - CORNER_MARGIN - height;
                textOnLeft = true;
                break;
            case RIGHT_STACK:
            default:
                top = rowTop(screenHeight, rightHeight, row) + row - height - 3;
                left = screenWidth / 2 + HALF_HOTBAR - width;
                textOnLeft = false;
                newRight = rightHeight + row;
                break;
        }
        return new BarLayout(left + xOffset, top - yOffset, width, height, textOnLeft, newRight, newLeft);
    }

    /**
     * The segment row: one vanilla row in the right column with its right end at the hotbar's right
     * edge (where the hunger row ends), moved by the segment offsets.
     *
     * @return {right end, icon top, Forge right height after the row}
     */
    static int[] segments(int xOffset, int yOffset, int screenWidth, int screenHeight, int rightHeight) {
        int top = screenHeight - rightHeight;
        return new int[] {screenWidth / 2 + HALF_HOTBAR + xOffset, top - yOffset, rightHeight + VANILLA_ROW};
    }

    /** Top of a row of the given size whose bottom meets the column's current height. */
    private static int rowTop(int screenHeight, int columnHeight, int row) {
        return screenHeight - columnHeight - (row - VANILLA_ROW);
    }
}
