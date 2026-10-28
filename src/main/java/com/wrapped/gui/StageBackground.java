package com.wrapped.gui;

import com.wrapped.compat.Gfx;
import java.util.Random;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

final class StageBackground {
    private static final int VIGNETTE_EDGE = 0x59000000;
    private static final int CLEAR = 0x00000000;
    private static final int RAY = 0x47FFFAD2;
    private static final int RAYS = 12;
    private static final int RAY_STEPS = 16;
    private static final long RAY_TURN_MS = 37700;
    private static final int SKY_TOP = 0xFF6A4A8C;
    private static final int SKY_MID = 0xFFC4527A;
    private static final int SKY_BOTTOM = 0xFFFF9A5A;
    private static final int MOON = 0xFFF4F1D0;
    private static final int CRATER = 0xFFD9D4A8;
    private static final long MOON_CROSS_MS = 30000;
    private static final int STARS = 60;
    private static final long STAR_SEED = 7;
    private static final int STAR = 0xFFFFFFFF;
    private static final float[] STAR_AT = stars();

    private StageBackground() {
    }

    private static float[] stars() {
        Random random = new Random(STAR_SEED);
        float[] at = new float[STARS * 2];
        for (int i = 0; i < at.length; i++) {
            at[i] = random.nextFloat();
        }
        return at;
    }

    static void draw(Gfx g, World world, int w, int h, long nowMs, boolean drift) {
        if (world.texture() == null) {
            sky(g, w, h, nowMs, drift);
            return;
        }
        tiles(g, world, w, h, nowMs, drift);
        g.fill(0, 0, w, h, world.overlay());
        if (world == World.GOLD && drift) {
            rays(g, w, h, nowMs);
        }
        vignette(g, w, h);
    }

    private static void tiles(Gfx g, World world, int w, int h, long nowMs, boolean drift) {
        Identifier texture = world.textureId();
        int tile = Ease.tileSize(w, h);
        float[] origin = Ease.tileOrigin(nowMs, tile, drift);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(origin[0], origin[1]);
        for (int x = 0; x < w + tile; x += tile) {
            for (int y = 0; y < h + tile; y += tile) {
                g.blit(texture, x, y, 0f, 0f, tile, tile, 16, 16, 16, 16,
                        world.textureTint());
            }
        }
        pose.popMatrix();
    }

    private static void rays(Gfx g, int w, int h, long nowMs) {
        int length = Math.max(w, h);
        double spread = length * 0.12;
        float base = (float) (2 * Math.PI * (nowMs % RAY_TURN_MS) / RAY_TURN_MS);
        Matrix3x2fStack pose = g.pose();
        for (int i = 0; i < RAYS; i++) {
            pose.pushMatrix();
            pose.translate(w / 2f, h * 0.42f);
            pose.rotate(base + (float) (i * 2 * Math.PI / RAYS));
            for (int k = 0; k < RAY_STEPS; k++) {
                int x0 = length * k / RAY_STEPS;
                int x1 = length * (k + 1) / RAY_STEPS;
                int half = (int) Math.round(spread * (k + 0.5) / RAY_STEPS);
                if (half > 0) {
                    g.fill(x0, -half, x1, half, RAY);
                }
            }
            pose.popMatrix();
        }
    }

    private static void vignette(Gfx g, int w, int h) {
        int t = Math.max(1, Math.round(Math.min(w, h) * 0.12f));
        g.fillGradient(0, 0, w, t, VIGNETTE_EDGE, CLEAR);
        g.fillGradient(0, h - t, w, h, CLEAR, VIGNETTE_EDGE);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(t, 0);
        pose.rotate((float) (Math.PI / 2));
        g.fillGradient(0, 0, h, t, CLEAR, VIGNETTE_EDGE);
        pose.popMatrix();
        pose.pushMatrix();
        pose.translate(w - t, h);
        pose.rotate((float) (-Math.PI / 2));
        g.fillGradient(0, 0, h, t, CLEAR, VIGNETTE_EDGE);
        pose.popMatrix();
    }

    private static void sky(Gfx g, int w, int h, long nowMs, boolean drift) {
        int mid = h / 2;
        g.fillGradient(0, 0, w, mid, SKY_TOP, SKY_MID);
        g.fillGradient(0, mid, w, h, SKY_MID, SKY_BOTTOM);
        for (int i = 0; i < STARS; i++) {
            int sx = (int) (STAR_AT[2 * i] * w);
            int sy = (int) (STAR_AT[2 * i + 1] * h * 0.5f);
            if (!drift || (nowMs / 400 + i) % 7 > 1) {
                g.fill(sx, sy, sx + 2, sy + 2, STAR);
            }
        }
        int m = Math.max(8, Math.round(Math.min(w, h) * 0.09f));
        double cross = drift ? (nowMs % MOON_CROSS_MS) / (double) MOON_CROSS_MS : 0.5;
        int mx = (int) Math.round(w * (0.15 + 0.7 * cross) - m / 2.0);
        double bob = drift ? Math.sin(nowMs / 3000.0) * Math.max(1, h / 60.0) : 0;
        int my = (int) Math.round(h * 0.1 - m / 2.0 + bob);
        my = Math.max(0, Math.min(Math.max(0, h / 5 - m), my));
        g.fill(mx, my, mx + m, my + m, MOON);
        int c1 = Math.max(1, Math.round(m * 0.25f));
        int c2 = Math.max(1, Math.round(m * 0.2f));
        int c1x = mx + Math.round(m * 0.2f);
        int c1y = my + Math.round(m * 0.25f);
        int c2x = mx + Math.round(m * 0.6f);
        int c2y = my + Math.round(m * 0.55f);
        g.fill(c1x, c1y, c1x + c1, c1y + c1, CRATER);
        g.fill(c2x, c2y, c2x + c2, c2y + c2, CRATER);
    }
}
