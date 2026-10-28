package com.wrapped.gui;

import com.wrapped.compat.Compat;
import com.wrapped.compat.CompatScreen;
import com.wrapped.compat.Gfx;
import java.util.function.Consumer;

public final class MergePrompt extends CompatScreen {
    static final String QUESTION = "Add this instance to your Total?";
    static final String DETAIL = "Total adds up your history from every instance you play on this account.";
    private static final int MAX_W = 220;
    private static final int PANEL_H = 96;
    private static final int PAD = 8;
    private static final int BUTTON_H = 20;

    private final CompatScreen parent;
    private final Consumer<Boolean> answer;

    public MergePrompt(CompatScreen parent, Consumer<Boolean> answer) {
        super(QUESTION);
        this.parent = parent;
        this.answer = answer;
    }

    private int panelW() {
        return Math.max(1, Math.min(MAX_W, width - 2 * Panels.MARGIN));
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    private int panelY() {
        return (height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int w = panelW();
        int half = (w - 2 * PAD - CardDraw.GAP) / 2;
        int by = panelY() + PANEL_H - PAD - BUTTON_H;
        addButton("Yes", panelX() + PAD, by, half, BUTTON_H, () -> choose(true));
        addButton("No", panelX() + w - PAD - half, by, half, BUTTON_H, () -> choose(false));
    }

    private void choose(boolean yes) {
        answer.accept(yes);
        Compat.setScreen(minecraft, parent);
    }

    @Override
    public void onClose() {
        Compat.setScreen(minecraft, parent);
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        int w = panelW();
        int x = panelX();
        int y = panelY();
        Panels.panel(g, x, y, w, PANEL_H);
        int cx = x + w / 2;
        int after = CardDraw.wrapped(g, font, QUESTION, cx, y + PAD, w - 2 * PAD, 2);
        CardDraw.wrapped(g, font, DETAIL, cx, after + 4, w - 2 * PAD, 3);
        drawWidgets(g, mouseX, mouseY, delta);
    }
}
