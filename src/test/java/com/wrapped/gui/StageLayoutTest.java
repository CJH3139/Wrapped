package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class StageLayoutTest {
    private static final int[][] SIZES = {
        {320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540},
        {1280, 720}, {2560, 1440}, {320, 480}, {360, 640}
    };
    private static final ToIntFunction<String> WIDTH = s -> s.length() * 6;

    private static boolean inside(Rect r, Rect outer) {
        return r.x() >= outer.x() && r.y() >= outer.y()
                && r.x() + r.w() <= outer.x() + outer.w()
                && r.y() + r.h() <= outer.y() + outer.h();
    }

    private static boolean overlaps(Rect a, Rect b) {
        return a.x() < b.x() + b.w() && b.x() < a.x() + a.w()
                && a.y() < b.y() + b.h() && b.y() < a.y() + a.h();
    }

    private static void assertDisjoint(List<Rect> rects, String where) {
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                assertFalse(overlaps(rects.get(i), rects.get(j)), where + " " + rects.get(i) + " " + rects.get(j));
            }
        }
    }

    @Test
    void everythingStaysOnScreen() {
        for (int[] s : SIZES) {
            int w = s[0];
            int h = s[1];
            String where = w + "x" + h;
            Rect safe = new Rect(4, 4, w - 8, h - 8);
            for (Rect r : List.of(StageLayout.progress(w), StageLayout.skip(w, h), StageLayout.motion(w, h))) {
                assertTrue(inside(r, safe), where + " chrome " + r);
            }
            Rect content = StageLayout.content(w, h);
            List<Rect> all = new ArrayList<>();
            int scale = StageLayout.titleYearScale(w, h, 24);
            StageLayout.Title t = StageLayout.title(w, h, 24, scale, true, true);
            List<Rect> titleRects = new ArrayList<>();
            titleRects.add(t.year());
            titleRects.add(t.previous().orElseThrow());
            titleRects.add(t.next().orElseThrow());
            titleRects.add(t.instance());
            titleRects.add(t.total());
            assertDisjoint(titleRects, where + " title");
            all.addAll(titleRects);
            List<Rect> slots = StageLayout.guessSlots(w, h, 3);
            assertDisjoint(slots, where + " slots");
            all.addAll(slots);
            for (int count = 1; count <= 4; count++) {
                for (int i = 0; i < count; i++) {
                    all.add(StageLayout.countdownRow(w, h, i, count));
                }
            }
            Rect card = StageLayout.shareCard(w, h);
            all.add(card);
            all.add(StageLayout.shareSave(card));
            all.add(StageLayout.hideWord(w, h));
            for (Rect r : all) {
                assertTrue(inside(r, content), where + " content " + r + " in " + content);
            }
        }
    }

    @Test
    void arrowsHideWithoutYears() {
        StageLayout.Title none = StageLayout.title(640, 360, 24, 4, false, false);
        assertTrue(none.previous().isEmpty());
        assertTrue(none.next().isEmpty());
        StageLayout.Title prev = StageLayout.title(640, 360, 24, 4, true, false);
        assertTrue(prev.previous().isPresent());
        assertTrue(prev.next().isEmpty());
    }

    @Test
    void hugeValuesFit() {
        assertEquals(Numbers.compact(99999), StageLayout.fitNumber(99999, 30, 1, WIDTH));
        assertEquals("1,218", StageLayout.fitNumber(1218, 200, 1, WIDTH));
        int avail = StageLayout.content(320, 240).w();
        assertTrue(WIDTH.applyAsInt(StageLayout.fitText("x".repeat(60), avail, WIDTH)) <= avail);
        assertEquals("short", StageLayout.fitText("short", avail, WIDTH));
        assertTrue(WIDTH.applyAsInt(StageLayout.fitNumber(1234567, 10, 1, WIDTH)) <= 10);
    }

    @Test
    void shareCardIsNineBySixteen() {
        for (int[] s : SIZES) {
            Rect card = StageLayout.shareCard(s[0], s[1]);
            assertTrue(Math.abs(card.w() * 16 - card.h() * 9) <= 16, s[0] + "x" + s[1] + " " + card);
        }
    }

    @Test
    void countdownRowsMatchRankedSize() {
        Rect content = StageLayout.content(320, 240);
        for (int count = 1; count <= 4; count++) {
            Rect previous = null;
            for (int i = 0; i < count; i++) {
                Rect r = StageLayout.countdownRow(320, 240, i, count);
                assertTrue(inside(r, content), count + " " + r);
                if (previous != null) {
                    assertTrue(r.y() >= previous.y() + previous.h(), count + " order " + i);
                }
                previous = r;
            }
        }
    }

    @Test
    void fitScaleFallsBackToOne() {
        assertEquals(1, StageLayout.fitScale(500, 100, 8));
        assertEquals(5, StageLayout.fitScale(20, 100, 8));
    }
}
