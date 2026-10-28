package com.wrapped.gui;

import java.util.Arrays;

final class ExportImage {
    static final int WIDTH = 1080;
    static final int HEIGHT = 1920;

    private ExportImage() {
    }

    static Rect crop(Rect gui, int scale, int fbW, int fbH) {
        int x0 = clamp(gui.x() * scale, fbW);
        int y0 = clamp(gui.y() * scale, fbH);
        int x1 = clamp((gui.x() + gui.w()) * scale, fbW);
        int y1 = clamp((gui.y() + gui.h()) * scale, fbH);
        return new Rect(x0, y0, x1 - x0, y1 - y0);
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }

    static Rect fit(int srcW, int srcH, int dstW, int dstH) {
        double s = Math.min((double) dstW / srcW, (double) dstH / srcH);
        int w = Math.max(1, Math.min(dstW, (int) Math.round(srcW * s)));
        int h = Math.max(1, Math.min(dstH, (int) Math.round(srcH * s)));
        return new Rect((dstW - w) / 2, (dstH - h) / 2, w, h);
    }

    static Rect wholeFit(int srcW, int srcH, int dstW, int dstH) {
        int k = Math.min(dstW / srcW, dstH / srcH);
        if (k < 1) {
            return fit(srcW, srcH, dstW, dstH);
        }
        int w = srcW * k;
        int h = srcH * k;
        return new Rect((dstW - w) / 2, (dstH - h) / 2, w, h);
    }

    static int[] scale(int[] src, int srcW, Rect crop, int corner, int dstW, int dstH, int fill) {
        int[] out = new int[dstW * dstH];
        Arrays.fill(out, fill);
        Rect to = wholeFit(crop.w(), crop.h(), dstW, dstH);
        for (int dy = 0; dy < to.h(); dy++) {
            int ly = (int) ((long) dy * crop.h() / to.h());
            boolean edgeY = ly < corner || ly >= crop.h() - corner;
            int row = (crop.y() + ly) * srcW + crop.x();
            int outRow = (to.y() + dy) * dstW + to.x();
            for (int dx = 0; dx < to.w(); dx++) {
                int lx = (int) ((long) dx * crop.w() / to.w());
                if (edgeY && (lx < corner || lx >= crop.w() - corner)) {
                    continue;
                }
                out[outRow + dx] = src[row + lx];
            }
        }
        return out;
    }
}
