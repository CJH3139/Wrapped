package com.wrapped.gui;

import com.wrapped.compat.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

final class StageDraw {
    static final int WHITE = 0xFFFFFFFF;
    static final int LINE = 9;
    private static final int PROGRESS_GAP = 3;
    private static final int PROGRESS_PAST = 0xFFFFFFFF;
    private static final int PROGRESS_FUTURE = 0x47FFFFFF;
    private static final int PROGRESS_SHADOW = 0x40000000;
    private static final float PROGRESS_STEP_MS = 4000f;
    private static final int STAMP_BORDER = 2;
    private static final int STAMP_PAD = 4;
    private static final int WIPE_COLOR = 0xFF2B2B2B;

    private StageDraw() {
    }

    static int giant(StageFrame f, String text, int cx, int y, int maxW, int maxScale, float punch) {
        Font font = f.font();
        int scale = StageLayout.fitScale(font.width(text), maxW, maxScale);
        String t = scale == 1 ? StageLayout.fitText(text, maxW, font::width) : text;
        int tw = font.width(t) * scale;
        int th = LINE * scale;
        Matrix3x2fStack pose = f.g().pose();
        pose.pushMatrix();
        pose.translate(cx, y + th / 2f);
        pose.scale(punch, punch);
        pose.translate(-tw / 2f, -th / 2f);
        pose.scale(scale, scale);
        f.g().text(font, t, 0, 0, WHITE, true);
        pose.popMatrix();
        return th;
    }

    static void label(StageFrame f, String text, int cx, int y) {
        Font font = f.font();
        Rect content = StageLayout.content(f.w(), f.h());
        int scale = content.w() >= 400 ? 2 : 1;
        String t = StageLayout.fitText(text, content.w() / scale, font::width);
        int tw = font.width(t) * scale;
        Matrix3x2fStack pose = f.g().pose();
        pose.pushMatrix();
        pose.translate(cx - tw / 2f, y);
        pose.scale(scale, scale);
        f.g().text(font, t, 0, 0, WHITE, true);
        pose.popMatrix();
    }

    static void item(StageFrame f, ItemStack stack, int cx, int cy, int size, long ms) {
        float dy = 0f;
        float degrees = 0f;
        if (!f.view().reduceMotion()) {
            Ease.Bob bob = Ease.bob(ms);
            dy = bob.dy() * size;
            degrees = bob.degrees();
        }
        float scale = size / 16f;
        Matrix3x2fStack pose = f.g().pose();
        pose.pushMatrix();
        pose.translate(cx, cy + dy);
        pose.rotate((float) Math.toRadians(degrees));
        pose.scale(scale, scale);
        pose.translate(-8f, -8f);
        f.g().item(stack, 0, 0);
        pose.popMatrix();
    }

    static void slot(StageFrame f, Rect r) {
        Gfx g = f.g();
        Panels.slot(g, r.x(), r.y(), r.w(), r.h());
        int x = r.x();
        int y = r.y();
        int right = x + r.w();
        int bottom = y + r.h();
        g.fill(x + 1, y + 1, right - 2, y + 2, Panels.SLOT_SHADOW);
        g.fill(x + 1, y + 1, x + 2, bottom - 2, Panels.SLOT_SHADOW);
        g.fill(x + 2, bottom - 2, right - 1, bottom - 1, Panels.LIGHT);
        g.fill(right - 2, y + 2, right - 1, bottom - 1, Panels.LIGHT);
    }

    static void stamp(StageFrame f, String text, int cx, int cy, int maxW, int maxScale, float degrees, int color,
            float pop) {
        Font font = f.font();
        int frame = 2 * (STAMP_BORDER + STAMP_PAD);
        int avail = Math.max(1, maxW - frame);
        int scale = StageLayout.fitScale(font.width(text), avail, maxScale);
        String t = scale == 1 ? StageLayout.fitText(text, avail, font::width) : text;
        int tw = font.width(t) * scale;
        int th = LINE * scale;
        int boxW = tw + frame;
        int boxH = th + frame;
        int left = -boxW / 2;
        int top = -boxH / 2;
        Gfx g = f.g();
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate((float) Math.toRadians(degrees));
        pose.scale(pop, pop);
        g.fill(left, top, left + boxW, top + STAMP_BORDER, color);
        g.fill(left, top + boxH - STAMP_BORDER, left + boxW, top + boxH, color);
        g.fill(left, top + STAMP_BORDER, left + STAMP_BORDER, top + boxH - STAMP_BORDER, color);
        g.fill(left + boxW - STAMP_BORDER, top + STAMP_BORDER, left + boxW, top + boxH - STAMP_BORDER, color);
        pose.translate(-tw / 2f, -th / 2f + scale / 2f);
        pose.scale(scale, scale);
        g.text(font, t, 0, 0, color, true);
        pose.popMatrix();
    }

    static void progress(StageFrame f) {
        int n = f.view().size();
        if (n <= 0) {
            return;
        }
        Gfx g = f.g();
        Rect bar = StageLayout.progress(f.w());
        int span = bar.w() + PROGRESS_GAP;
        int current = f.view().index();
        float fill = Math.max(0f, Math.min(1f, f.view().stepElapsedMs() / PROGRESS_STEP_MS));
        int top = bar.y();
        int bottom = bar.y() + bar.h();
        for (int i = 0; i < n; i++) {
            int x0 = bar.x() + span * i / n;
            int x1 = Math.max(x0 + 1, bar.x() + span * (i + 1) / n - PROGRESS_GAP);
            g.fill(x0 + 1, top + 1, x1 + 1, bottom + 1, PROGRESS_SHADOW);
            if (i < current) {
                g.fill(x0, top, x1, bottom, PROGRESS_PAST);
                continue;
            }
            g.fill(x0, top, x1, bottom, PROGRESS_FUTURE);
            if (i == current) {
                int filled = Math.round((x1 - x0) * fill);
                if (filled > 0) {
                    g.fill(x0, top, x0 + filled, bottom, PROGRESS_PAST);
                }
            }
        }
    }

    static void hint(StageFrame f, String text) {
        Font font = f.font();
        String t = StageLayout.fitText(text, Math.max(1, f.w() - 2 * StageLayout.MARGIN), font::width);
        f.g().text(font, t, (f.w() - font.width(t)) / 2, StageLayout.hintY(f.h()), WHITE, true);
    }

    static void particles(StageFrame f, Particles p) {
        Gfx g = f.g();
        p.each((px, py, size, base, life) -> {
            int alpha = Math.round((base >>> 24) * Math.max(0f, Math.min(1f, life)));
            if (alpha <= 0) {
                return;
            }
            int x = Math.round(px);
            int y = Math.round(py);
            g.fill(x, y, x + size, y + size, (alpha << 24) | (base & 0x00FFFFFF));
        });
    }

    static void wipe(StageFrame f, BlockWipe wipe) {
        Gfx g = f.g();
        int cell = Math.max(24, Math.min(f.w(), f.h()) / 8);
        int cols = (f.w() + cell - 1) / cell + 1;
        int rows = (f.h() + cell - 1) / cell + 1;
        long now = f.nowMs();
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                float s = wipe.cellScale(i, j, cols, rows, now);
                if (s <= 0f) {
                    continue;
                }
                int d = (int) Math.ceil(cell * s);
                int x = i * cell + (cell - d) / 2;
                int y = j * cell + (cell - d) / 2;
                g.fill(x, y, x + d, y + d, WIPE_COLOR);
            }
        }
    }
}
