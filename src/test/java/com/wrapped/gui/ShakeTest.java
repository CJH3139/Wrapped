package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShakeTest {
    private static boolean anyNonZero(Shake s, long from, long to, double scale) {
        for (long t = from; t <= to; t += 16) {
            Shake.Offset o = s.offset(t, scale);
            if (o.x() != 0 || o.y() != 0) {
                return true;
            }
        }
        return false;
    }

    @Test
    void decaysToZero() {
        Shake s = new Shake();
        s.start(0, 300);
        assertTrue(anyNonZero(s, 0, 80, 1));
        Shake.Offset o = s.offset(299, 1);
        assertTrue(Math.abs(o.x()) <= 6 && Math.abs(o.y()) <= 6);
        assertEquals(new Shake.Offset(0, 0), s.offset(300, 1));
    }

    @Test
    void zeroScaleNeverShakes() {
        Shake s = new Shake();
        s.start(0, 300);
        for (long t = 0; t < 300; t += 16) {
            assertEquals(new Shake.Offset(0, 0), s.offset(t, 0));
        }
    }

    @Test
    void shorterShakeKeepsLaterEnd() {
        Shake s = new Shake();
        s.start(0, 300);
        s.start(100, 50);
        assertTrue(anyNonZero(s, 160, 290, 100));
        assertEquals(new Shake.Offset(0, 0), s.offset(300, 100));
    }
}
