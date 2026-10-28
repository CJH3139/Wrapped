package com.wrapped.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

final class CardDraw {
    static final int INK = 0xFF404040;
    static final int WHITE = 0xFFFFFFFF;
    static final int GAP = 2;
    static final int GLYPH_H = 8;
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HOVER = Identifier.withDefaultNamespace("widget/button_highlighted");

    private CardDraw() {
    }

    static String fit(Font font, String s, int maxW) {
        return TextFit.ellipsize(s, maxW, font::width);
    }

    static int wrapped(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int maxW, int maxLines) {
        List<FormattedCharSequence> lines = font.split(Component.literal(s), Math.max(1, maxW));
        int shown = Math.min(maxLines, lines.size());
        for (int i = 0; i < shown; i++) {
            FormattedCharSequence line = lines.get(i);
            g.text(font, line, cx - font.width(line) / 2, y + i * font.lineHeight, INK, false);
        }
        return y + shown * font.lineHeight;
    }

    static void button(GuiGraphicsExtractor g, Font font, String text, Rect r, int mouseX, int mouseY) {
        boolean hovered = r.contains(mouseX, mouseY);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? BUTTON_HOVER : BUTTON, r.x(), r.y(), r.w(), r.h());
        String t = fit(font, text, r.w() - 6);
        g.text(font, t, r.x() + (r.w() - font.width(t)) / 2, r.y() + (r.h() - GLYPH_H) / 2, WHITE, true);
    }

    static void highlight(GuiGraphicsExtractor g, Rect r, int color) {
        g.fill(r.x() + 1, r.y() + 1, r.x() + r.w() - 1, r.y() + r.h() - 1, color);
    }
}
