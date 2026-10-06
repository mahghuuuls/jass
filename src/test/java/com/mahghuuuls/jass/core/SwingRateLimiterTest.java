package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingRateLimiterTest {

    @Test
    void reportsAtTheVanillaRateAreAllAccepted() {
        SwingRateLimiter limiter = new SwingRateLimiter(10, 1);
        for (long tick = 0; tick < 200; tick += 10) {
            assertTrue(limiter.tryAccept(tick));
        }
    }

    @Test
    void anEarlyJitteredReportIsDroppedNotCharged() {
        SwingRateLimiter limiter = new SwingRateLimiter(10, 1);
        assertTrue(limiter.tryAccept(0));
        assertFalse(limiter.tryAccept(7));
        assertTrue(limiter.tryAccept(17));
    }

    @Test
    void floodingNeverExceedsTheVanillaRate() {
        SwingRateLimiter limiter = new SwingRateLimiter(10, 1);
        int accepted = 0;
        for (long tick = 0; tick < 100; tick++) {
            if (limiter.tryAccept(tick)) {
                accepted++;
            }
        }
        // Vanilla allows misses at ticks 0, 10, ..., 90: ten in 100 ticks.
        assertEquals(10, accepted);
    }

    @Test
    void aDuplicateReportInTheSameTickIsNotChargedTwice() {
        SwingRateLimiter limiter = new SwingRateLimiter(10, 1);
        assertTrue(limiter.tryAccept(5));
        assertFalse(limiter.tryAccept(5));
    }
}
