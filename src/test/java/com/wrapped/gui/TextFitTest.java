package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

    @Test
    void wrapBreaksAtSpaces() {
        assertEquals(List.of("Add this", "instance to", "your Total?"),
                TextFit.wrap("Add this instance to your Total?", 66, SIX));
    }

    @Test
    void wrapKeepsShortTextOnOneLine() {
        assertEquals(List.of("Before we start..."), TextFit.wrap("Before we start...", 200, SIX));
    }

    @Test
    void wrapSplitsWordsLongerThanALine() {
        assertEquals(List.of("abcd", "efgh", "ij k"), TextFit.wrap("abcdefghij k", 24, SIX));
    }

    @Test
    void wrapCollapsesExtraSpaces() {
        assertEquals(List.of("a b"), TextFit.wrap("  a   b  ", 100, SIX));
        assertEquals(List.of(), TextFit.wrap("   ", 100, SIX));
    }

    @Test
    void wrapNeverExceedsWidthUnlessOneCharIsWider() {
        String s = "Total adds up your history from every instance you play on this account.";
        for (int w = 6; w < 300; w++) {
            for (String line : TextFit.wrap(s, w, SIX)) {
                assertTrue(SIX.applyAsInt(line) <= w, line + " at " + w);
            }
            assertEquals(s.replace(" ", ""), String.join("", TextFit.wrap(s, w, SIX)).replace(" ", ""));
        }
        assertEquals(List.of("a", "b"), TextFit.wrap("ab", 3, SIX));
    }
}
