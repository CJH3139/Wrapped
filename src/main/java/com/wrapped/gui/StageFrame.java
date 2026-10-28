package com.wrapped.gui;

import com.wrapped.compat.Gfx;
import net.minecraft.client.gui.Font;

record StageFrame(Gfx g, Font font, int w, int h, int mx, int my, long elapsedMs, long nowMs,
        StageView view) {
}
