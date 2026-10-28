package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EaseTest {
    @Test
    void punchSettles() {
        assertEquals(1.22f, Ease.punch(0), 0.001f);
        assertEquals(1f, Ease.punch(350), 0.0001f);
        assertEquals(1f, Ease.punch(-5), 0.0001f);
    }

    @Test
    void dropHiddenThenLands() {
        assertTrue(Float.isNaN(Ease.drop(-1)));
        assertEquals(0f, Ease.drop(500), 0.0001f);
        assertEquals(-1.2f, Ease.drop(0), 0.0001f);
        assertEquals(0.06f, Ease.drop(350), 0.0001f);
    }

    @Test
    void tileGeometry() {
        assertEquals(40, Ease.tileSize(320, 240));
        assertEquals(24, Ease.tileSize(100, 100));
        assertArrayEquals(new float[] {-20f, -20f}, Ease.tileOrigin(6000, 40, true), 0.0001f);
        assertArrayEquals(new float[] {0f, 0f}, Ease.tileOrigin(6000, 40, false), 0.0001f);
        assertArrayEquals(new float[] {-20.5f, -20.3f}, Ease.tileOrigin(6030, 40, true), 0.0001f);
    }

    @Test
    void bobStaysSmall() {
        for (long ms = 0; ms <= 4800; ms += 100) {
            Ease.Bob b = Ease.bob(ms);
            assertTrue(Math.abs(b.dy()) <= 0.04f + 1e-6f);
            assertTrue(Math.abs(b.degrees()) <= 6f + 1e-5f);
        }
    }

    @Test
    void slidePopAndFadeEndpoints() {
        assertEquals(-0.3f, Ease.slide(0), 0.0001f);
        assertEquals(0f, Ease.slide(450), 0.0001f);
        assertEquals(0.4f, Ease.pop(0), 0.0001f);
        assertEquals(1f, Ease.pop(450), 0.0001f);
        assertEquals(0.5f, Ease.fade(50, 100), 0.0001f);
        assertEquals(1f, Ease.fade(500, 100), 0.0001f);
        assertEquals(0f, Ease.fade(-5, 100), 0.0001f);
    }
}
