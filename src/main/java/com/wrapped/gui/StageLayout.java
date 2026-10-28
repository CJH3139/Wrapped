package com.wrapped.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

final class StageLayout {
    static final int MARGIN = 16;
    private static final int LINE = 9;
    private static final int ARROW = 16;
    private static final int ARROW_GAP = 6;
    private static final int TAB_W = 60;
    private static final int TAB_H = 16;
    private static final int TITLE_GAP = 12;
    private static final int SLOT_H = 40;
    private static final int SLOT_GAP = 8;
    private static final int LABEL_H = 20;
    private static final int ROW_GAP = 6;

    record Title(Rect year, Optional<Rect> previous, Optional<Rect> next, Rect instance, Rect total) {
    }

    private StageLayout() {
    }

    static Rect content(int w, int h) {
        return new Rect(MARGIN, 20, w - 2 * MARGIN, h - 44);
    }

    static int fitScale(int textWidth, int avail, int maxScale) {
        for (int s = maxScale; s >= 2; s--) {
            if ((long) textWidth * s <= avail) {
                return s;
            }
        }
        return 1;
    }

    static String fitNumber(long value, int avail, int scale, ToIntFunction<String> width) {
        String grouped = Numbers.grouped(value);
        if (width.applyAsInt(grouped) * scale <= avail) {
            return grouped;
        }
        String compact = Numbers.compact(value);
        if (width.applyAsInt(compact) * scale <= avail) {
            return compact;
        }
        return TextFit.ellipsize(compact, avail / Math.max(1, scale), width);
    }

    static String fitText(String text, int avail, ToIntFunction<String> width) {
        return TextFit.ellipsize(text, avail, width);
    }

    static Rect progress(int w) {
        return new Rect(MARGIN, 6, w - 2 * MARGIN, 2);
    }

    static int hintY(int h) {
        return h - 14;
    }

    static Rect skip(int w, int h) {
        return new Rect(w - 4 - 40, h - 4 - 14, 40, 14);
    }

    static Rect motion(int w, int h) {
        return new Rect(4, h - 4 - 14, 64, 14);
    }

    static int titleYearScale(int w, int h, int yearTextWidth) {
        Rect c = content(w, h);
        int max = Math.max(1, Math.min(c.h() / 3 / LINE, 12));
        return fitScale(yearTextWidth, c.w() - 2 * (ARROW + ARROW_GAP), max);
    }

    static Title title(int w, int h, int yearTextWidth, int yearScale, boolean hasPrevious, boolean hasNext) {
        Rect c = content(w, h);
        int yw = yearTextWidth * yearScale;
        int yh = LINE * yearScale;
        int group = yh + TITLE_GAP + TAB_H;
        int top = c.y() + Math.max(0, (c.h() - group) / 2);
        Rect year = new Rect(c.x() + (c.w() - yw) / 2, top, yw, yh);
        int arrowY = year.y() + (yh - ARROW) / 2;
        Optional<Rect> previous = hasPrevious
                ? Optional.of(new Rect(year.x() - ARROW_GAP - ARROW, arrowY, ARROW, ARROW))
                : Optional.empty();
        Optional<Rect> next = hasNext
                ? Optional.of(new Rect(year.x() + yw + ARROW_GAP, arrowY, ARROW, ARROW))
                : Optional.empty();
        int tabsX = c.x() + (c.w() - 2 * TAB_W) / 2;
        int tabsY = year.y() + yh + TITLE_GAP;
        return new Title(year, previous, next,
                new Rect(tabsX, tabsY, TAB_W, TAB_H), new Rect(tabsX + TAB_W, tabsY, TAB_W, TAB_H));
    }

    static List<Rect> guessSlots(int w, int h, int count) {
        Rect c = content(w, h);
        boolean row = w >= h;
        int slotW = row ? Math.min(c.w() / count - SLOT_GAP, 120) : Math.min(c.w(), 200);
        int groupW = row ? count * slotW + (count - 1) * SLOT_GAP : slotW;
        int groupH = row ? SLOT_H : count * SLOT_H + (count - 1) * SLOT_GAP;
        int bandTop = c.y() + c.h() * 2 / 5;
        int bandH = c.y() + c.h() - bandTop;
        int x0 = c.x() + (c.w() - groupW) / 2;
        int y0 = bandTop + Math.max(0, (bandH - groupH) / 2);
        List<Rect> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (row) {
                slots.add(new Rect(x0 + i * (slotW + SLOT_GAP), y0, slotW, SLOT_H));
            } else {
                slots.add(new Rect(x0, y0 + i * (SLOT_H + SLOT_GAP), slotW, SLOT_H));
            }
        }
        return slots;
    }

    static Rect countdownRow(int w, int h, int i, int count) {
        Rect c = content(w, h);
        int avail = c.h() - LABEL_H;
        int rowH = Math.min(40, (avail - ROW_GAP * (count - 1)) / count);
        return new Rect(c.x(), c.y() + LABEL_H + i * (rowH + ROW_GAP), c.w(), rowH);
    }

    static Rect shareCard(int w, int h) {
        Rect c = content(w, h);
        int ch = c.h();
        int cw = ch * 9 / 16;
        if (cw > c.w()) {
            cw = c.w();
            ch = cw * 16 / 9;
        }
        return new Rect(c.x() + (c.w() - cw) / 2, c.y() + (c.h() - ch) / 2, cw, ch);
    }

    static Rect shareSave(Rect card) {
        return new Rect(card.x() + 8, card.y() + card.h() - 8 - 16, card.w() - 16, 16);
    }

    static Rect hideWord(int w, int h) {
        Rect c = content(w, h);
        return new Rect(c.x() + (c.w() - 80) / 2, c.y() + c.h() - 16, 80, 16);
    }
}
