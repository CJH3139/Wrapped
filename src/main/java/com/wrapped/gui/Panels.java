package com.wrapped.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Panels {
    static final int FACE = 0xFFC6C6C6;
    static final int LIGHT = 0xFFFFFFFF;
    static final int SHADOW = 0xFF555555;
    static final int OUTLINE = 0xFF000000;
    static final int SLOT_FACE = 0xFF8B8B8B;
    static final int SLOT_SHADOW = 0xFF373737;
    static final int MARGIN = 8;

    private Panels() {
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int r = x + w;
        int b = y + h;
        g.fill(x + 1, y, r - 1, y + 1, OUTLINE);
        g.fill(x + 1, b - 1, r - 1, b, OUTLINE);
        g.fill(x, y + 1, x + 1, b - 1, OUTLINE);
        g.fill(r - 1, y + 1, r, b - 1, OUTLINE);
        g.fill(x + 1, y + 1, r - 1, b - 1, FACE);
        g.fill(x + 1, y + 1, r - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, b - 3, LIGHT);
        g.fill(x + 3, b - 3, r - 1, b - 1, SHADOW);
        g.fill(r - 3, y + 3, r - 1, b - 1, SHADOW);
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int r = x + w;
        int b = y + h;
        g.fill(x, y, r, b, SLOT_FACE);
        g.fill(x, y, r - 1, y + 1, SLOT_SHADOW);
        g.fill(x, y, x + 1, b - 1, SLOT_SHADOW);
        g.fill(x + 1, b - 1, r, b, LIGHT);
        g.fill(r - 1, y + 1, r, b, LIGHT);
    }
}
