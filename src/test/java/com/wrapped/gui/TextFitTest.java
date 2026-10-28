package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class TextFitTest {
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;

    @Test
    void keepsTextThatFits() {
        assertEquals("hypixel.net", TextFit.ellipsize("hypixel.net", 66, SIX));
    }

    @Test
    void trimsWithEllipsis() {
        assertEquals("hypix...", TextFit.ellipsize("hypixel.net", 48, SIX));
    }

    @Test
    void neverExceedsWidth() {
        String s = "a.really.long.server.name.example.com";
        for (int w = 0; w < 300; w++) {
            assertEquals(true, SIX.applyAsInt(TextFit.ellipsize(s, w, SIX)) <= w);
        }
    }

    @Test
    void tooNarrowForEllipsisGivesEmpty() {
        assertEquals("", TextFit.ellipsize("hypixel.net", 10, SIX));
        assertEquals("...", TextFit.ellipsize("hypixel.net", 18, SIX));
    }
}
