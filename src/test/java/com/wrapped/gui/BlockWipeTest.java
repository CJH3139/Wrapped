package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockWipeTest {
    @Test
    void coversThenSwapsThenClears() {
        BlockWipe w = new BlockWipe(0);
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 5; r++) {
                assertEquals(0f, w.cellScale(c, r, 8, 5, 0), 0.0001f);
            }
        }
        assertEquals(1f, w.cellScale(0, 0, 8, 5, 260), 0.0001f);
        assertTrue(w.swapDue(260));
        w.markSwapped();
        assertFalse(w.swapDue(260));
        assertTrue(w.swapped());
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 5; r++) {
                assertEquals(0f, w.cellScale(c, r, 8, 5, 520), 0.0001f);
            }
        }
        assertTrue(w.done(520));
        assertFalse(w.done(519));
    }

    @Test
    void finishEndsAtOnce() {
        BlockWipe w = new BlockWipe(0);
        w.finish();
        assertTrue(w.swapped());
        assertTrue(w.done(11));
        assertFalse(w.swapDue(300));
    }
}
