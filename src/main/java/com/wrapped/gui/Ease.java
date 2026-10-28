package com.wrapped.gui;

final class Ease {
    private Ease() {
    }

    record Bob(float dy, float degrees) {
    }

    private static final double BOB_PERIOD_MS = 2400;

    static float punch(long msSinceLand) {
        if (msSinceLand < 0 || msSinceLand >= 350) {
            return 1f;
        }
        double inv = 1 - msSinceLand / 350.0;
        return (float) (1 + 0.22 * inv * inv);
    }

    static float drop(long msSinceStart) {
        if (msSinceStart < 0) {
            return Float.NaN;
        }
        if (msSinceStart >= 500) {
            return 0f;
        }
        if (msSinceStart < 350) {
            double t = msSinceStart / 350.0;
            return (float) (-1.2 + 1.26 * t * t);
        }
        double t = (msSinceStart - 350) / 150.0;
        double inv = 1 - t;
        return (float) (0.06 * inv * inv);
    }

    static float slide(long ms) {
        if (ms >= 450) {
            return 0f;
        }
        double t = Math.max(0, ms) / 450.0;
        return (float) (-0.3 * (1 - outBack(t, 1.70158)));
    }

    static float pop(long ms) {
        if (ms >= 450) {
            return 1f;
        }
        double t = Math.max(0, ms) / 450.0;
        return (float) (0.4 + 0.6 * outBack(t, 1.0));
    }

    static float fade(long ms, long durationMs) {
        if (durationMs <= 0) {
            return 1f;
        }
        return (float) Math.max(0, Math.min(1, (double) ms / durationMs));
    }

    static Bob bob(long ms) {
        double phase = 2 * Math.PI * ms / BOB_PERIOD_MS;
        return new Bob((float) (0.04 * Math.sin(phase)), (float) (6 * Math.cos(phase)));
    }

    static int tileSize(int w, int h) {
        return Math.max(24, Math.min(w, h) / 6);
    }

    static float[] tileOrigin(long nowMs, int tile, boolean drift) {
        if (!drift) {
            return new float[] {0f, 0f};
        }
        float x = (float) ((nowMs / 60.0) % tile);
        float y = (float) ((nowMs * 0.6 / 60.0) % tile);
        return new float[] {-x, -y};
    }

    private static double outBack(double t, double c1) {
        double c3 = c1 + 1;
        double u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }
}
