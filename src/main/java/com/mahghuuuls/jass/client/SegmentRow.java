package com.mahghuuuls.jass.client;

/**
 * Which of the ten segments are full, half, or empty (REQ-056), as plain arithmetic so it can be
 * tested without a game. Segment 0 is the rightmost: the row fills right to left, like hunger.
 */
final class SegmentRow {

    static final int SEGMENTS = 10;
    static final int EMPTY = 0;
    static final int HALF = 1;
    static final int FULL = 2;

    private SegmentRow() {
    }

    /** Filled half segments, 0 to 20, rounded down; debt and a missing maximum give 0. */
    static int halves(float visible, float maximum) {
        if (!(maximum > 0.0F) || !(visible > 0.0F)) {
            return 0;
        }
        int halves = (int) Math.floor(visible * SEGMENTS * 2 / maximum);
        return Math.max(0, Math.min(SEGMENTS * 2, halves));
    }

    /** The state of one segment, counting from the right. */
    static int state(int segment, int halves) {
        int filled = halves - segment * 2;
        return filled >= 2 ? FULL : filled == 1 ? HALF : EMPTY;
    }
}
