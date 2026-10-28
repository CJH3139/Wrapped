package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CountUpTest {
    @Test
    void startsAtZero() {
        assertEquals(0, CountUp.valueAt(214, 0, 1000));
    }

    @Test
    void reachesTargetAtDuration() {
        assertEquals(214, CountUp.valueAt(214, 1000, 1000));
    }

    @Test
    void holdsTargetAfterDuration() {
        assertEquals(214, CountUp.valueAt(214, 2000, 1000));
    }

    @Test
    void monotonicAndNeverAboveTarget() {
        long previous = Long.MIN_VALUE;
        for (long t = 0; t <= 1000; t++) {
            long v = CountUp.valueAt(214, t, 1000);
            assertTrue(v >= previous);
            assertTrue(v <= 214);
            previous = v;
        }
    }

    @Test
    void easesOutAheadOfLinear() {
        assertTrue(CountUp.valueAt(1000, 500, 1000) > 500);
    }

    @Test
    void zeroDurationShowsTarget() {
        assertEquals(42, CountUp.valueAt(42, 0, 0));
    }

    @Test
    void negativeElapsedShowsZero() {
        assertEquals(0, CountUp.valueAt(42, -10, 1000));
    }

    @Test
    void largeTargetsStayBounded() {
        long big = 9_000_000_000_000L;
        assertEquals(big, CountUp.valueAt(big, 1000, 1000));
        assertTrue(CountUp.valueAt(big, 999, 1000) <= big);
    }
}
