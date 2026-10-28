package com.wrapped.gui;

public final class CountUp {
    private CountUp() {
    }

    public static long valueAt(long target, long elapsedMs, long durationMs) {
        if (durationMs <= 0 || elapsedMs >= durationMs) {
            return target;
        }
        if (elapsedMs <= 0) {
            return 0;
        }
        double remaining = 1.0 - (double) elapsedMs / durationMs;
        double eased = 1.0 - remaining * remaining * remaining;
        return Math.min(target, (long) Math.floor(target * eased));
    }
}
