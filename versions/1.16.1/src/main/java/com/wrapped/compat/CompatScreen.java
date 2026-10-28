package com.wrapped.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public abstract class CompatScreen extends Screen {
    protected CompatScreen(String title) {
        super(new TextComponent(title));
    }

    protected abstract void draw(Gfx g, int mouseX, int mouseY, float delta);

    protected boolean onClick(double x, double y, int button) {
        return false;
    }

    protected boolean onKey(int key) {
        return false;
    }

    protected final void addButton(String label, int x, int y, int w, int h, Runnable onPress) {
        addButton(new Button(x, y, w, h, new TextComponent(label), b -> onPress.run()));
    }

    protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g.raw());
    }

    protected final void drawWidgets(Gfx g, int mouseX, int mouseY, float delta) {
        super.render(g.raw(), mouseX, mouseY, delta);
    }

    @Override
    public final void render(PoseStack g, int mouseX, int mouseY, float delta) {
        Gfx gfx = new Gfx(g);
        drawBackground(gfx, mouseX, mouseY, delta);
        draw(gfx, mouseX, mouseY, delta);
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        return super.mouseClicked(x, y, button) || onClick(x, y, button);
    }

    @Override
    public final boolean keyPressed(int key, int scanCode, int modifiers) {
        return onKey(key) || super.keyPressed(key, scanCode, modifiers);
    }
}
