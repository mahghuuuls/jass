package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.client.JassClientConfig.BarPosition;
import com.mahghuuuls.jass.client.JassClientConfig.FillDirection;

/**
 * Where the bar style draws this frame, as plain arithmetic so it can be tested without a game.
 *
 * <p>Stack positions take one row of the vanilla bar column: rows are 10 pixels like vanilla's,
 * or taller for a tall bar, and start above the column's current height (which already includes
 * Classic Bar's bars: Classic Bar advances Forge's heights for each bar it draws). Offsets move only
 * the bar; the reserved row stays where it was, so other bars keep their places.
 *
 * <p>Anchored positions sit a small gap from each screen edge they touch and are centred along an
 * axis where they touch no edge; they leave the column heights alone. Whatever the position and
 * offsets, the bar and its 1-pixel border stay on screen; a bar larger than the screen starts at the
 * screen's left or top edge.
 */
final class BarLayout {

    /** Pixels between the screen edge and an anchored bar's fill area; the 1-pixel border uses one of them. */
    private static final int EDGE_MARGIN = 3;
    /** Pixels between the bar's fill area and its numbers. */
    private static final int TEXT_GAP = 3;
    private static final int FONT_HEIGHT = 9;
    private static final int BORDER = 1;
    private static final int VANILLA_ROW = 10;
    /** Half the hotbar width: the bar columns end at the screen centre plus or minus this. */
    private static final int HALF_HOTBAR = 91;

    final int left;
    final int top;
    final int width;
    final int height;
    /** True when the numbers normally go on the bar's left, away from the screen edge or the hotbar. */
    final boolean textOnLeft;
    private final int screenWidth;
    /** Forge's right and left column heights after this bar (unchanged for anchored positions). */
    final int rightHeight;
    final int leftHeight;

    private BarLayout(int left, int top, int width, int height, boolean textOnLeft, int rightHeight, int leftHeight,
            int screenWidth) {
        this.screenWidth = screenWidth;
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
        int left;
        int top;
        boolean textOnLeft;
        int newRight = rightHeight;
        int newLeft = leftHeight;
        if (position.isStack()) {
            int row = Math.max(VANILLA_ROW, height + 5);
            boolean leftColumn = position == BarPosition.LEFT_STACK;
            int columnHeight = leftColumn ? leftHeight : rightHeight;
            top = rowTop(screenHeight, columnHeight, row) + row - height - 3;
            left = leftColumn ? screenWidth / 2 - HALF_HOTBAR : screenWidth / 2 + HALF_HOTBAR - width;
            textOnLeft = leftColumn;
            if (leftColumn) {
                newLeft = leftHeight + row;
            } else {
                newRight = rightHeight + row;
            }
        } else {
            left = anchored(position.column, width, screenWidth);
            top = anchored(position.row, height, screenHeight);
            textOnLeft = position.column == 2;
        }
        int x = clamp(left + xOffset, BORDER, screenWidth - BORDER - width);
        int y = clamp(top - yOffset, BORDER, screenHeight - BORDER - height);
        return new BarLayout(x, y, width, height, textOnLeft, newRight, newLeft, screenWidth);
    }

    /** The filled part of the bar for this Stamina; nothing is filled when the maximum is not above 0. */
    int[] fill(FillDirection direction, float visible, float maximum) {
        return fillForShare(direction, maximum > 0.0F ? visible / maximum : 0.0F);
    }

    /**
     * Left edge of numbers of the given width: on their usual side, or on the other side when only
     * that side keeps them on screen.
     */
    int textX(int textWidth) {
        int leftSide = left - TEXT_GAP - textWidth;
        int rightSide = left + width + TEXT_GAP;
        boolean fitsLeft = leftSide >= 0;
        boolean fitsRight = rightSide + textWidth <= screenWidth;
        if (textOnLeft) {
            return fitsLeft || !fitsRight ? leftSide : rightSide;
        }
        return fitsRight || !fitsLeft ? rightSide : leftSide;
    }

    /** Top of the numbers: vertically centred on the bar. */
    int textY() {
        return top + (height - FONT_HEIGHT) / 2;
    }

    /**
     * The filled part of the bar for a share of 0 to 1, as {left, top, right, bottom}: it starts at
     * the direction's start and grows toward the other end. Width or height is 0 for an empty bar.
     */
    int[] fillForShare(FillDirection direction, float share) {
        float clamped = share > 0.0F ? Math.min(1.0F, share) : 0.0F;
        int right = left + width;
        int bottom = top + height;
        switch (direction) {
            case RIGHT_TO_LEFT:
                return new int[] {right - Math.round(width * clamped), top, right, bottom};
            case BOTTOM_TO_TOP:
                return new int[] {left, bottom - Math.round(height * clamped), right, bottom};
            case TOP_TO_BOTTOM:
                return new int[] {left, top, right, top + Math.round(height * clamped)};
            case LEFT_TO_RIGHT:
            default:
                return new int[] {left, top, left + Math.round(width * clamped), bottom};
        }
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

    /** The near edge of an anchored bar on one axis: 0 start, 1 centre (rounded down), 2 end. */
    private static int anchored(int anchor, int size, int screenSize) {
        switch (anchor) {
            case 0:
                return EDGE_MARGIN;
            case 2:
                return screenSize - EDGE_MARGIN - size;
            default:
                return Math.floorDiv(screenSize - size, 2);
        }
    }

    /** Keeps a value in [minimum, maximum]; when the range is empty (a bar larger than the screen) it is the minimum. */
    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    /** Top of a row of the given size whose bottom meets the column's current height. */
    private static int rowTop(int screenHeight, int columnHeight, int row) {
        return screenHeight - columnHeight - (row - VANILLA_ROW);
    }
}
