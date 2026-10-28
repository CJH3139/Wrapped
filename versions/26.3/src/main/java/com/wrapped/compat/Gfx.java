package com.wrapped.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public final class Gfx {
    private final GuiGraphicsExtractor g;

    Gfx(GuiGraphicsExtractor g) {
        this.g = g;
    }

    GuiGraphicsExtractor raw() {
        return g;
    }

    public Matrix3x2fStack pose() {
        return g.pose();
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        g.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        g.text(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        g.text(font, text, x, y, color, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        g.item(stack, x, y);
    }

    public void blit(Identifier texture, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
            int textureW, int textureH, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, regionW, regionH, textureW, textureH, color);
    }

    public void blitSprite(Identifier sprite, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        g.enableScissor(x0, y0, x1, y1);
    }

    public void disableScissor() {
        g.disableScissor();
    }
}
