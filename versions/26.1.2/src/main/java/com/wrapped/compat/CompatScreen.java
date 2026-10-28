package com.wrapped.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class CompatScreen extends Screen {
    protected CompatScreen(Component title) {
        super(title);
    }

    protected abstract void draw(Gfx g, int mouseX, int mouseY, float delta);

    protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g.raw(), mouseX, mouseY, delta);
    }

    protected final void drawWidgets(Gfx g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g.raw(), mouseX, mouseY, delta);
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        draw(new Gfx(g), mouseX, mouseY, delta);
    }

    @Override
    public final void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        drawBackground(new Gfx(g), mouseX, mouseY, delta);
    }
}
