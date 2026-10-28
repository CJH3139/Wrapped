package com.wrapped.gui;

import java.util.Random;

final class Shake {
    record Offset(int x, int y) {
    }

    private static final double AMPLITUDE = 6;

    private long end;
    private long duration;

    void start(long nowMs, int durationMs) {
        long newEnd = nowMs + durationMs;
        if (newEnd <= end) {
            return;
        }
        end = newEnd;
        duration = durationMs;
    }

    Offset offset(long nowMs, double scale) {
        if (scale <= 0 || duration <= 0 || nowMs >= end) {
            return new Offset(0, 0);
        }
        double remaining = Math.max(0, Math.min(1, (double) (end - nowMs) / duration));
        double amplitude = AMPLITUDE * remaining * scale;
        Random random = new Random(nowMs / 16);
        int x = (int) Math.round((random.nextDouble() * 2 - 1) * amplitude);
        int y = (int) Math.round((random.nextDouble() * 2 - 1) * amplitude);
        return new Offset(x, y);
    }
}
