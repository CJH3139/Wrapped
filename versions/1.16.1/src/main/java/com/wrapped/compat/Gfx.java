package com.wrapped.compat;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class Gfx {
    private static final ResourceLocation WIDGETS = new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final Painter PAINTER = new Painter();

    private static final class Painter extends GuiComponent {
        void gradient(PoseStack pose, int x0, int y0, int x1, int y1, int top, int bottom) {
            fillGradient(pose, x0, y0, x1, y1, top, bottom);
        }
    }

    private final PoseStack pose;

    Gfx(PoseStack pose) {
        this.pose = pose;
    }

    PoseStack raw() {
        return pose;
    }

    public void push() {
        pose.pushPose();
    }

    public void pop() {
        pose.popPose();
    }

    public void translate(float x, float y) {
        pose.translate(x, y, 0.0);
    }

    public void scale(float x, float y) {
        pose.scale(x, y, 1f);
    }

    public void rotate(float radians) {
        pose.mulPose(Vector3f.ZP.rotation(radians));
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        GuiComponent.fill(pose, x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        PAINTER.gradient(pose, x0, y0, x1, y1, top, bottom);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void item(ItemStack stack, int x, int y) {
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(pose.last().pose());
        Minecraft.getInstance().getItemRenderer().renderGuiItem(stack, x, y);
        RenderSystem.popMatrix();
    }

    public void blit(Tex texture, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
            int textureW, int textureH, int color) {
        Minecraft.getInstance().getTextureManager().bind(texture.id);
        RenderSystem.color4f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f,
                ((color >>> 24) & 0xFF) / 255f);
        GuiComponent.blit(pose, x, y, w, h, u, v, regionW, regionH, textureW, textureH);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    public void button(int x, int y, int w, int h, boolean hovered) {
        widget(x, y, w, h, 46 + (hovered ? 2 : 1) * 20);
    }

    public void tab(int x, int y, int w, int h, boolean selected, boolean hovered) {
        widget(x, y, w, h, selected ? 66 : hovered ? 86 : 46);
    }

    private void widget(int x, int y, int w, int h, int v) {
        Minecraft.getInstance().getTextureManager().bind(WIDGETS);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int left = Math.min(w / 2, 100);
        int right = Math.min(w - left, 100);
        int top = Math.min(h / 2, 10);
        int bottom = Math.min(h - top, 10);
        GuiComponent.blit(pose, x, y, 0, v, left, top, 256, 256);
        GuiComponent.blit(pose, x + w - right, y, 200 - right, v, right, top, 256, 256);
        GuiComponent.blit(pose, x, y + h - bottom, 0, v + 20 - bottom, left, bottom, 256, 256);
        GuiComponent.blit(pose, x + w - right, y + h - bottom, 200 - right, v + 20 - bottom, right, bottom, 256, 256);
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int sx = (int) Math.round(x0 * scale);
        int sy = (int) Math.round(window.getHeight() - y1 * scale);
        int sw = Math.max(0, (int) Math.round((x1 - x0) * scale));
        int sh = Math.max(0, (int) Math.round((y1 - y0) * scale));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
