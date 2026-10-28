package com.wrapped.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class CompatScreen extends Screen {
    protected CompatScreen(String title) {
        super(Component.literal(title));
    }

    protected abstract void draw(Gfx g, int mouseX, int mouseY, float delta);

    protected boolean onClick(double x, double y, int button) {
        return false;
    }

    protected boolean onKey(int key) {
        return false;
    }

    protected final void addButton(String label, int x, int y, int w, int h, Runnable onPress) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> onPress.run()).bounds(x, y, w, h).build());
    }

    protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g.raw(), mouseX, mouseY, delta);
    }

    protected final void drawWidgets(Gfx g, int mouseX, int mouseY, float delta) {
        super.render(g.raw(), mouseX, mouseY, delta);
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        draw(new Gfx(g), mouseX, mouseY, delta);
    }

    @Override
    public final void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        drawBackground(new Gfx(g), mouseX, mouseY, delta);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return super.mouseClicked(event, doubled) || onClick(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        return onKey(event.key()) || super.keyPressed(event);
    }
}
