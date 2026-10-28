package com.wrapped.gui;

import com.wrapped.compat.Gfx;
import java.util.List;
import net.minecraft.client.gui.Font;

final class CardDraw {
    static final int INK = 0xFF404040;
    static final int WHITE = 0xFFFFFFFF;
    static final int GAP = 2;
    static final int GLYPH_H = 8;

    private CardDraw() {
    }

    static String fit(Font font, String s, int maxW) {
        return TextFit.ellipsize(s, maxW, font::width);
    }

    static int wrapped(Gfx g, Font font, String s, int cx, int y, int maxW, int maxLines) {
        List<String> lines = TextFit.wrap(s, Math.max(1, maxW), font::width);
        int shown = Math.min(maxLines, lines.size());
        for (int i = 0; i < shown; i++) {
            String line = lines.get(i);
            g.text(font, line, cx - font.width(line) / 2, y + i * font.lineHeight, INK, false);
        }
        return y + shown * font.lineHeight;
    }

    static void button(Gfx g, Font font, String text, Rect r, int mouseX, int mouseY) {
        boolean hovered = r.contains(mouseX, mouseY);
        g.button(r.x(), r.y(), r.w(), r.h(), hovered);
        String t = fit(font, text, r.w() - 6);
        g.text(font, t, r.x() + (r.w() - font.width(t)) / 2, r.y() + (r.h() - GLYPH_H) / 2, WHITE, true);
    }

    static void highlight(Gfx g, Rect r, int color) {
        g.fill(r.x() + 1, r.y() + 1, r.x() + r.w() - 1, r.y() + r.h() - 1, color);
    }
}
