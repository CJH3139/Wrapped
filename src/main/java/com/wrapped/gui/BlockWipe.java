package com.wrapped.gui;

final class BlockWipe {
    static final long DURATION_MS = 520;

    private final long startMs;
    private boolean swapped;
    private boolean finished;

    BlockWipe(long startMs) {
        this.startMs = startMs;
    }

    private double progress(long nowMs) {
        return (double) (nowMs - startMs) / DURATION_MS;
    }

    boolean swapDue(long nowMs) {
        return !swapped && progress(nowMs) >= 0.5;
    }

    void markSwapped() {
        swapped = true;
    }

    boolean swapped() {
        return swapped;
    }

    boolean done(long nowMs) {
        return finished || progress(nowMs) >= 1;
    }

    void finish() {
        swapped = true;
        finished = true;
    }

    float cellScale(int col, int row, int cols, int rows, long nowMs) {
        if (finished) {
            return 0f;
        }
        double k = progress(nowMs);
        double order = (col + row) / (double) (cols + rows);
        double s;
        if (k < 0.5) {
            s = (2 * k - 0.6 * order) / 0.4;
        } else {
            s = 1 - ((2 * (k - 0.5)) - 0.6 * order) / 0.4;
        }
        return (float) Math.max(0, Math.min(1, s));
    }
}
