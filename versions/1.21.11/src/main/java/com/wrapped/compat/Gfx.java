package com.wrapped.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class Gfx {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HOVER = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier TAB = Identifier.withDefaultNamespace("widget/tab");
    private static final Identifier TAB_HOVER = Identifier.withDefaultNamespace("widget/tab_highlighted");
    private static final Identifier TAB_SELECTED = Identifier.withDefaultNamespace("widget/tab_selected");
    private static final Identifier TAB_SELECTED_HOVER = Identifier.withDefaultNamespace("widget/tab_selected_highlighted");

    private final GuiGraphics g;

    Gfx(GuiGraphics g) {
        this.g = g;
    }

    GuiGraphics raw() {
        return g;
    }

    public void push() {
        g.pose().pushMatrix();
    }

    public void pop() {
        g.pose().popMatrix();
    }

    public void translate(float x, float y) {
        g.pose().translate(x, y);
    }

    public void scale(float x, float y) {
        g.pose().scale(x, y);
    }

    public void rotate(float radians) {
        g.pose().rotate(radians);
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        g.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, color, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        g.renderItem(stack, x, y);
    }

    public void blit(Tex texture, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
            int textureW, int textureH, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture.id, x, y, u, v, w, h, regionW, regionH, textureW, textureH, color);
    }

    public void button(int x, int y, int w, int h, boolean hovered) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, (hovered ? BUTTON_HOVER : BUTTON), x, y, w, h);
    }

    public void tab(int x, int y, int w, int h, boolean selected, boolean hovered) {
        Identifier sprite = selected ? (hovered ? TAB_SELECTED_HOVER : TAB_SELECTED) : (hovered ? TAB_HOVER : TAB);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        g.enableScissor(x0, y0, x1, y1);
    }

    public void disableScissor() {
        g.disableScissor();
    }
}
