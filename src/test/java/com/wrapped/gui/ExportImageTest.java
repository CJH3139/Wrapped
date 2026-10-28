package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExportImageTest {
    private static final int FILL = 0xFFC6C6C6;

    @Test
    void cropMultipliesGuiCoordinatesByScale() {
        assertEquals(new Rect(318, 48, 216, 384), ExportImage.crop(new Rect(159, 24, 108, 192), 2, 854, 480));
    }

    @Test
    void cropClampsToFramebuffer() {
        assertEquals(new Rect(300, 0, 100, 50), ExportImage.crop(new Rect(100, -5, 50, 30), 3, 400, 50));
    }

    @Test
    void cropOfOffscreenCardIsEmpty() {
        Rect r = ExportImage.crop(new Rect(500, 500, 10, 10), 2, 400, 300);
        assertEquals(0, r.w() * r.h());
    }

    @Test
    void exactAspectFillsTarget() {
        assertEquals(new Rect(0, 0, 1080, 1920), ExportImage.fit(216, 384, 1080, 1920));
    }

    @Test
    void widerSourceLetterboxesTopAndBottom() {
        Rect r = ExportImage.fit(200, 200, 1080, 1920);
        assertEquals(new Rect(0, 420, 1080, 1080), r);
    }

    @Test
    void tallerSourceLetterboxesSides() {
        assertEquals(new Rect(300, 0, 480, 1920), ExportImage.fit(100, 400, 1080, 1920));
    }

    @Test
    void fitNeverExceedsTarget() {
        for (int[] s : new int[][] {{216, 385}, {217, 384}, {108, 192}, {1, 1}, {3, 1000}, {1000, 3}}) {
            Rect r = ExportImage.fit(s[0], s[1], 1080, 1920);
            assertTrue(r.x() >= 0 && r.y() >= 0 && r.x() + r.w() <= 1080 && r.y() + r.h() <= 1920);
            assertTrue(r.w() >= 1 && r.h() >= 1);
        }
    }

    @Test
    void nearestNeighbourDoublesEachPixel() {
        int[] src = {1, 2, 3, 4};
        int[] out = ExportImage.scale(src, 2, new Rect(0, 0, 2, 2), 0, 4, 4, FILL);
        assertArrayEquals(new int[] {1, 1, 2, 2, 1, 1, 2, 2, 3, 3, 4, 4, 3, 3, 4, 4}, out);
    }

    @Test
    void scaleReadsOnlyTheCropFromTheFullFrame() {
        int[] src = {
                9, 9, 9, 9,
                9, 1, 2, 9,
                9, 3, 4, 9,
        };
        int[] out = ExportImage.scale(src, 4, new Rect(1, 1, 2, 2), 0, 2, 2, FILL);
        assertArrayEquals(new int[] {1, 2, 3, 4}, out);
    }

    @Test
    void letterboxUsesFill() {
        int[] src = {5, 6};
        int[] out = ExportImage.scale(src, 2, new Rect(0, 0, 2, 1), 0, 2, 3, FILL);
        assertArrayEquals(new int[] {FILL, FILL, 5, 6, FILL, FILL}, out);
    }

    @Test
    void cornerBlocksAreFilledSoTheRoundedPanelCornersAreClean() {
        int[] src = new int[16];
        java.util.Arrays.fill(src, 7);
        int[] out = ExportImage.scale(src, 4, new Rect(0, 0, 4, 4), 1, 4, 4, FILL);
        assertEquals(FILL, out[0]);
        assertEquals(FILL, out[3]);
        assertEquals(FILL, out[12]);
        assertEquals(FILL, out[15]);
        assertEquals(7, out[1]);
        assertEquals(7, out[4]);
        assertEquals(7, out[5]);
    }

    @Test
    void cornerBlockSizeFollowsGuiScale() {
        int[] src = new int[36];
        java.util.Arrays.fill(src, 7);
        int[] out = ExportImage.scale(src, 6, new Rect(0, 0, 6, 6), 2, 6, 6, FILL);
        assertEquals(FILL, out[7]);
        assertEquals(7, out[2]);
        assertEquals(7, out[12]);
        assertEquals(FILL, out[35 - 7]);
    }

    @Test
    void upscaleUsesTheLargestWholeFactorAndLetterboxesTheRest() {
        int[] src = {1, 2, 3, 4};
        int[] out = ExportImage.scale(src, 2, new Rect(0, 0, 2, 2), 0, 5, 5, FILL);
        assertArrayEquals(new int[] {
                1, 1, 2, 2, FILL,
                1, 1, 2, 2, FILL,
                3, 3, 4, 4, FILL,
                3, 3, 4, 4, FILL,
                FILL, FILL, FILL, FILL, FILL}, out);
    }

    @Test
    void wholeFactorFit() {
        assertEquals(new Rect(0, 0, 1080, 1920), ExportImage.wholeFit(216, 384, 1080, 1920));
        assertEquals(new Rect(54, 96, 972, 1728), ExportImage.wholeFit(324, 576, 1080, 1920));
        assertEquals(new Rect(40, 460, 1000, 1000), ExportImage.wholeFit(200, 200, 1080, 1920));
        assertEquals(new Rect(0, 0, 1080, 1920), ExportImage.wholeFit(1080, 1920, 1080, 1920));
    }

    @Test
    void sourceLargerThanTargetStillFits() {
        Rect r = ExportImage.wholeFit(2160, 3000, 1080, 1920);
        assertTrue(r.x() >= 0 && r.y() >= 0 && r.x() + r.w() <= 1080 && r.y() + r.h() <= 1920);
        assertTrue(r.w() >= 1 && r.h() >= 1);
    }
}
