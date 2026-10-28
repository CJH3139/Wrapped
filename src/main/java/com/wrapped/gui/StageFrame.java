package com.wrapped.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

record StageFrame(GuiGraphicsExtractor g, Font font, int w, int h, int mx, int my, long elapsedMs, long nowMs,
        StageView view) {
}
