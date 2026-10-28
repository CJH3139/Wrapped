package com.wrapped.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

final class StageKit {
    static final long REST = Long.MAX_VALUE / 4;
    static final int WHITE = 0xFFFFFFFF;
    static final int INK = 0xFF404040;
    static final int GOLD = 0xFFFFD84A;
    static final int SLOT_LABEL = 0xFFE0E0E0;
    static final int HOVER = 0x80FFFFFF;
    static final long FADE_MS = 400;
    static final int LINE = StageDraw.LINE;
    private static final int STAMP_FRAME = 12;
    private static final int BOX_PAD = 5;
    private static final int ICON = 16;
    private static final int BOX_GAP = 2;
    private static final int MIN_ALPHA = 8;

    enum Kind {
        TEXT, BIG, BOX
    }

    record Part(Kind kind, String text, int maxScale, int slots, int boxW, int boxH) {
        static Part text(String text, int maxScale) {
            return new Part(Kind.TEXT, text, Math.max(1, maxScale), 1, 0, 0);
        }

        static Part big(String measure, int maxScale, int slots) {
            return new Part(Kind.BIG, measure, Math.max(1, maxScale), Math.max(1, slots), 0, 0);
        }

        static Part box(int w, int h) {
            return new Part(Kind.BOX, "", 1, 1, Math.max(0, w), Math.max(0, h));
        }
    }

    record Hero(Rect item, int x, int w, boolean left, int gap, int[] y, int[] scale, List<Part> parts) {
        int cx() {
            return x + w / 2;
        }

        int cellW(int i) {
            return cellWidth(w, parts.get(i).slots(), gap);
        }

        int cellX(int i, int slot) {
            return x + slot * (cellW(i) + 2 * gap);
        }

        int height(int i) {
            return partHeight(parts.get(i), scale[i]);
        }
    }

    record Stamp(int scale, int maxW, int boundW, int boundH) {
    }

    private StageKit() {
    }

    static long elapsed(StageFrame f) {
        return f.view().exporting() ? REST : Math.max(0, f.elapsedMs());
    }

    static long stepElapsed(StageFrame f) {
        return f.view().exporting() ? REST : Math.max(0, f.view().stepElapsedMs());
    }

    static Rect content(StageFrame f) {
        return StageLayout.content(f.w(), f.h());
    }

    static int gap(Rect c) {
        return Math.max(4, Math.min(16, c.h() / 28));
    }

    static int labelScale(Rect c) {
        return c.w() >= 400 ? 2 : 1;
    }

    static int faded(float alpha, int rgb) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        if (a < MIN_ALPHA) {
            return 0;
        }
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    static boolean live(StageFrame f) {
        return !f.view().exporting();
    }

    static boolean hovered(StageFrame f, Rect r) {
        return live(f) && r.contains(f.mx(), f.my());
    }

    static void scaled(StageFrame f, String text, int x, int y, int scale, int color, boolean shadow) {
        if (color == 0 || text.isEmpty()) {
            return;
        }
        Matrix3x2fStack pose = f.g().pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        f.g().text(f.font(), text, 0, 0, color, shadow);
        pose.popMatrix();
    }

    static void scaled(StageFrame f, FormattedCharSequence text, int x, int y, int scale, int color) {
        if (color == 0) {
            return;
        }
        Matrix3x2fStack pose = f.g().pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        f.g().text(f.font(), text, 0, 0, color, true);
        pose.popMatrix();
    }

    static void centered(StageFrame f, String text, int cx, int y, int maxW, int scale, int color, boolean shadow) {
        Font font = f.font();
        String t = StageLayout.fitText(text, Math.max(1, maxW / scale), font::width);
        scaled(f, t, cx - font.width(t) * scale / 2, y, scale, color, shadow);
    }

    static int textScale(Font font, String text, int maxW, int maxScale) {
        return StageLayout.fitScale(font.width(text), maxW, Math.max(1, maxScale));
    }

    static String number(Font font, long value, int maxW, int scale) {
        return StageLayout.fitNumber(value, Math.max(1, maxW), scale, font::width);
    }

    static void inScale(StageFrame f, float cx, float cy, float s, Runnable draw) {
        Matrix3x2fStack pose = f.g().pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(s, s);
        pose.translate(-cx, -cy);
        draw.run();
        pose.popMatrix();
    }

    static int boxHeight(int valueScale) {
        return BOX_PAD + LINE + BOX_GAP + LINE * valueScale + BOX_PAD - 1;
    }

    static int boxInner(int boxW) {
        return Math.max(1, boxW - 2 * BOX_PAD);
    }

    static int boxInner(int boxW, boolean icon) {
        return Math.max(1, boxInner(boxW) - (icon ? ICON + BOX_PAD : 0));
    }

    static void box(StageFrame f, Rect r, String label, String value, int valueScale) {
        box(f, r, label, value, valueScale, ItemStack.EMPTY);
    }

    static void box(StageFrame f, Rect r, String label, String value, int valueScale, ItemStack icon) {
        Font font = f.font();
        StageDraw.slot(f, r);
        boolean withIcon = !icon.isEmpty();
        int inner = boxInner(r.w(), withIcon);
        if (withIcon) {
            f.g().item(icon, r.x() + r.w() - BOX_PAD - ICON, r.y() + (r.h() - ICON) / 2);
        }
        String l = StageLayout.fitText(label, inner, font::width);
        f.g().text(font, l, r.x() + BOX_PAD, r.y() + BOX_PAD, SLOT_LABEL, true);
        String v = StageLayout.fitText(value, Math.max(1, inner / valueScale), font::width);
        scaled(f, v, r.x() + BOX_PAD, r.y() + BOX_PAD + LINE + BOX_GAP, valueScale, WHITE, true);
    }

    static Stamp stamp(Font font, String text, int maxW, int maxScale, float degrees, int maxH) {
        double rad = Math.toRadians(Math.abs(degrees));
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        int avail = Math.max(1, maxW - STAMP_FRAME);
        int scale = StageLayout.fitScale(font.width(text), avail, Math.max(1, maxScale));
        while (true) {
            int tw = scale == 1 ? Math.min(font.width(text), avail) : font.width(text) * scale;
            int bw = tw + STAMP_FRAME;
            int bh = LINE * scale + STAMP_FRAME;
            int boundW = (int) Math.ceil(bw * cos + bh * sin);
            int boundH = (int) Math.ceil(bw * sin + bh * cos);
            if (scale == 1 || (boundH <= maxH && boundW <= maxW)) {
                return new Stamp(scale, maxW, boundW, boundH);
            }
            scale--;
        }
    }

    static void drawStamp(StageFrame f, String text, Stamp st, int cx, int cy, float degrees, int color, float pop) {
        StageDraw.stamp(f, text, cx, cy, st.maxW(), st.scale(), degrees, color, pop);
    }

    static int heroSize(Rect c) {
        return Math.max(16, (int) Math.min(c.h() * 0.6, c.w() * 0.3));
    }

    static int columnWidth(StageFrame f, boolean withItem) {
        Rect c = content(f);
        if (withItem && f.w() >= f.h()) {
            return Math.max(1, c.w() - heroSize(c) - 2 * gap(c));
        }
        return c.w();
    }

    static Hero hero(StageFrame f, boolean withItem, int reserveBottom, List<Part> parts) {
        Font font = f.font();
        Rect c = content(f);
        int gap = gap(c);
        boolean side = withItem && f.w() >= f.h();
        int size = withItem ? heroSize(c) : 0;
        int colW = columnWidth(f, withItem);
        int availH = Math.max(1, c.h() - reserveBottom);
        int stackAvail = side || !withItem ? availH : Math.max(1, availH - size - gap);
        int n = parts.size();
        int[] scale = new int[n];
        for (int i = 0; i < n; i++) {
            Part p = parts.get(i);
            scale[i] = switch (p.kind()) {
                case TEXT -> textScale(font, p.text(), colW, p.maxScale());
                case BIG -> textScale(font, p.text(), cellWidth(colW, p.slots(), gap), p.maxScale());
                case BOX -> 1;
            };
        }
        while (stackHeight(parts, scale, gap) > stackAvail && shrink(parts, scale)) {
            continue;
        }
        int stackH = stackHeight(parts, scale, gap);
        int textW = 1;
        for (int i = 0; i < n; i++) {
            textW = Math.max(textW, partWidth(font, parts.get(i), scale[i], gap));
        }
        textW = Math.min(textW, colW);
        Rect item;
        int x;
        int top;
        boolean left;
        if (side) {
            int groupW = size + 2 * gap + textW;
            int groupH = Math.max(size, stackH);
            int gx = c.x() + Math.max(0, (c.w() - groupW) / 2);
            int gy = c.y() + Math.max(0, (availH - groupH) / 2);
            item = new Rect(gx, gy + (groupH - size) / 2, size, size);
            x = gx + size + 2 * gap;
            top = gy + (groupH - stackH) / 2;
            left = true;
        } else {
            int head = withItem ? size + gap : 0;
            int groupH = head + stackH;
            int gy = c.y() + Math.max(0, (availH - groupH) / 2);
            item = withItem ? new Rect(c.x() + (c.w() - size) / 2, gy, size, size) : new Rect(0, 0, 0, 0);
            x = c.x() + (c.w() - textW) / 2;
            top = gy + head;
            left = false;
        }
        int[] y = new int[n];
        int cursor = top;
        for (int i = 0; i < n; i++) {
            y[i] = cursor;
            cursor += partHeight(parts.get(i), scale[i]) + gap;
        }
        return new Hero(item, x, textW, left, gap, y, scale, List.copyOf(parts));
    }

    static void heroItem(StageFrame f, Hero h, ItemStack stack) {
        Rect r = h.item();
        if (r.w() <= 0) {
            return;
        }
        StageDraw.item(f, stack, r.x() + r.w() / 2, r.y() + r.h() / 2, r.w(), f.nowMs());
    }

    static void heroText(StageFrame f, Hero h, int i, String text, int color) {
        if (color == 0) {
            return;
        }
        Font font = f.font();
        int s = h.scale()[i];
        String t = StageLayout.fitText(text, Math.max(1, h.w() / s), font::width);
        int tw = font.width(t) * s;
        int x = h.left() ? h.x() : h.x() + (h.w() - tw) / 2;
        scaled(f, t, x, h.y()[i], s, color, true);
    }

    static int heroCellCenter(StageFrame f, Hero h, int i, int slot, String text) {
        Part p = h.parts().get(i);
        int cellW = h.cellW(i);
        if (p.slots() == 1 && h.left()) {
            int tw = Math.min(cellW, f.font().width(text) * h.scale()[i]);
            return h.x() + tw / 2;
        }
        return h.cellX(i, slot) + cellW / 2;
    }

    static String heroNumber(StageFrame f, Hero h, int i, long value) {
        return number(f.font(), value, h.cellW(i), h.scale()[i]);
    }

    static void heroBig(StageFrame f, Hero h, int i, int slot, String text, float punch) {
        int cx = heroCellCenter(f, h, i, slot, text);
        StageDraw.giant(f, text, cx, h.y()[i], h.cellW(i), h.scale()[i], punch);
    }

    static float[] heroBigCenter(StageFrame f, Hero h, int i, int slot, String text) {
        int cx = heroCellCenter(f, h, i, slot, text);
        return new float[] {cx, h.y()[i] + LINE * h.scale()[i] / 2f};
    }

    static float[] center(StageFrame f) {
        Rect c = content(f);
        return new float[] {c.x() + c.w() / 2f, c.y() + c.h() / 2f};
    }

    private static int cellWidth(int w, int slots, int gap) {
        return Math.max(1, (w - (slots - 1) * 2 * gap) / slots);
    }

    private static int partHeight(Part p, int scale) {
        return p.kind() == Kind.BOX ? p.boxH() : LINE * scale;
    }

    private static int partWidth(Font font, Part p, int scale, int gap) {
        return switch (p.kind()) {
            case TEXT -> font.width(p.text()) * scale;
            case BIG -> p.slots() * font.width(p.text()) * scale + (p.slots() - 1) * 2 * gap;
            case BOX -> p.boxW();
        };
    }

    private static int stackHeight(List<Part> parts, int[] scale, int gap) {
        int total = 0;
        for (int i = 0; i < parts.size(); i++) {
            total += partHeight(parts.get(i), scale[i]);
        }
        return total + Math.max(0, parts.size() - 1) * gap;
    }

    private static boolean shrink(List<Part> parts, int[] scale) {
        if (shrinkLargest(parts, scale, Kind.BIG, 4)) {
            return true;
        }
        if (shrinkLargest(parts, scale, Kind.TEXT, 1)) {
            return true;
        }
        return shrinkLargest(parts, scale, Kind.BIG, 1);
    }

    private static boolean shrinkLargest(List<Part> parts, int[] scale, Kind kind, int floor) {
        int pick = -1;
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).kind() == kind && scale[i] > floor && (pick < 0 || scale[i] > scale[pick])) {
                pick = i;
            }
        }
        if (pick < 0) {
            return false;
        }
        scale[pick]--;
        return true;
    }
}
