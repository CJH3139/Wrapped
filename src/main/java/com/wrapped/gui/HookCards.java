package com.wrapped.gui;

import com.wrapped.cards.Card;
import com.wrapped.gui.StageKit.Hero;
import com.wrapped.gui.StageKit.Part;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

final class HookCards {
    static final String TITLE_LABEL = "Your year in Minecraft";
    static final String GUESS_LABEL = "Before we start...";
    static final String QUESTION = "How many hours do you think you played?";
    static final String HOURS_LABEL = "You played";
    private static final long DIGIT_START_MS = 250;
    private static final long DIGIT_STEP_MS = 220;
    private static final long LAND_MS = 2600;
    private static final int GUESS_MAX_SCALE = 3;
    private static final int HOURS_MAX_SCALE = 12;
    private static final Identifier TAB = Identifier.withDefaultNamespace("widget/tab");
    private static final Identifier TAB_HOVER = Identifier.withDefaultNamespace("widget/tab_highlighted");
    private static final Identifier TAB_SELECTED = Identifier.withDefaultNamespace("widget/tab_selected");
    private static final Identifier TAB_SELECTED_HOVER =
            Identifier.withDefaultNamespace("widget/tab_selected_highlighted");
    private static final int TAB_IDLE_TEXT = 0xFFA0A0A0;
    private static final int DIVIDER_W = 48;
    private static final int DIVIDER_H = 2;
    private static final int DIVIDER = 0x99FFFFFF;
    private static final int PICK_OUTLINE = 0xFFFFFFFF;
    private static final int GUESS_UNIT = 0xFFE0E0E0;
    private static final int BIG = 1;

    private HookCards() {
    }

    static void title(StageFrame f, Card.Title c) {
        long t = StageKit.elapsed(f);
        Font font = f.font();
        Rect content = StageKit.content(f);
        String year = Integer.toString(c.year());
        int scale = yearScale(f, year);
        StageLayout.Title layout = titleLayout(f, year, scale);
        Rect yr = layout.year();
        int labelH = StageKit.LINE * StageKit.labelScale(content);
        int labelY = Math.max(content.y(), yr.y() - labelH - StageKit.gap(content));
        StageDraw.label(f, TITLE_LABEL, content.x() + content.w() / 2, labelY);
        Matrix3x2fStack pose = f.g().pose();
        for (int i = 0; i < year.length(); i++) {
            float drop = Ease.drop(t - (DIGIT_START_MS + i * DIGIT_STEP_MS));
            if (Float.isNaN(drop)) {
                continue;
            }
            String digit = year.substring(i, i + 1);
            int dx = yr.x() + font.width(year.substring(0, i)) * scale;
            pose.pushMatrix();
            pose.translate(dx, Math.max(0f, yr.y() + drop * yr.h()));
            pose.scale(scale, scale);
            f.g().text(font, digit, 0, 0, StageKit.WHITE, true);
            pose.popMatrix();
        }
        if (!StageKit.live(f)) {
            return;
        }
        layout.previous().ifPresent(r -> CardDraw.button(f.g(), font, "<", r, f.mx(), f.my()));
        layout.next().ifPresent(r -> CardDraw.button(f.g(), font, ">", r, f.mx(), f.my()));
        tab(f, "Instance", layout.instance(), !f.view().total());
        tab(f, "Total", layout.total(), f.view().total());
    }

    static void guess(StageFrame f, Card.Guess c) {
        Font font = f.font();
        Rect content = StageKit.content(f);
        int gap = StageKit.gap(content);
        List<Rect> slots = c.options().isEmpty() ? List.of()
                : StageLayout.guessSlots(f.w(), f.h(), c.options().size());
        int labelScale = StageKit.labelScale(content);
        int labelH = StageKit.LINE * labelScale;
        int regionBottom = slots.isEmpty() ? content.y() + content.h() * 2 / 5 : slots.get(0).y() - gap;
        int regionH = Math.max(0, regionBottom - content.y());
        int qs = 2;
        List<FormattedCharSequence> lines = font.split(Component.literal(QUESTION), Math.max(1, content.w() / qs));
        if (lines.size() > 2 || labelH + gap + lines.size() * StageKit.LINE * qs > regionH) {
            qs = 1;
            lines = font.split(Component.literal(QUESTION), Math.max(1, content.w()));
        }
        int groupH = labelH + gap + lines.size() * StageKit.LINE * qs;
        int top = content.y() + Math.max(0, (regionH - groupH) / 2);
        int cx = content.x() + content.w() / 2;
        StageDraw.label(f, GUESS_LABEL, cx, top);
        int ly = top + labelH + gap;
        for (FormattedCharSequence line : lines) {
            StageKit.scaled(f, line, cx - font.width(line) * qs / 2, ly, qs, StageKit.WHITE);
            ly += StageKit.LINE * qs;
        }
        Optional<Integer> pick = f.view().guessPick();
        GuiGraphicsExtractor g = f.g();
        for (int i = 0; i < slots.size(); i++) {
            int option = c.options().get(i);
            Rect r = slots.get(i);
            StageDraw.slot(f, r);
            boolean picked = pick.filter(p -> p == option).isPresent();
            if (picked) {
                CardDraw.highlight(g, r, StageKit.HOVER);
                outline(g, r);
            } else if (pick.isEmpty() && StageKit.hovered(f, r)) {
                CardDraw.highlight(g, r, StageKit.HOVER);
            }
            int inner = Math.max(1, r.w() - 8);
            int maxScale = Math.max(1, Math.min(GUESS_MAX_SCALE, (r.h() - StageKit.LINE) / StageKit.LINE));
            int s = StageKit.textScale(font, Numbers.grouped(option), inner, maxScale);
            String num = StageKit.number(font, option, inner, s);
            int blockH = StageKit.LINE * s + StageKit.LINE;
            int by = r.y() + (r.h() - blockH) / 2 + 1;
            int rcx = r.x() + r.w() / 2;
            StageDraw.giant(f, num, rcx, by, inner, s, 1f);
            StageKit.centered(f, option == 1 ? "hour" : "hours", rcx, by + StageKit.LINE * s, inner, 1, GUESS_UNIT,
                    true);
        }
    }

    static void hoursReveal(StageFrame f, Card.HoursReveal c) {
        long t = StageKit.elapsed(f);
        Hero h = hoursLayout(f, c);
        StageKit.heroItem(f, h, new ItemStack(Items.CLOCK));
        StageKit.heroText(f, h, 0, HOURS_LABEL, StageKit.WHITE);
        long value = CardTiming.count(c).map(w -> CardTiming.countValue(w, t)).orElse(c.hours());
        StageKit.heroBig(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, value), Ease.punch(t - LAND_MS));
        StageKit.heroText(f, h, 2, c.hours() == 1 ? "hour" : "hours", StageKit.GOLD);
        float fade = Ease.fade(t - LAND_MS, StageKit.FADE_MS);
        List<String> subs = hoursLines(f, c);
        if (subs.isEmpty()) {
            return;
        }
        int dw = Math.min(DIVIDER_W, h.w());
        int dx = h.left() ? h.x() : h.cx() - dw / 2;
        int divider = StageKit.faded(fade * 0.6f, DIVIDER);
        if (divider != 0) {
            f.g().fill(dx, h.y()[3], dx + dw, h.y()[3] + DIVIDER_H, divider);
        }
        boolean guessed = f.view().guessPick().isPresent();
        for (int i = 0; i < subs.size(); i++) {
            boolean last = guessed && i == subs.size() - 1;
            StageKit.heroText(f, h, 4 + i, subs.get(i), StageKit.faded(fade, last ? StageKit.GOLD : StageKit.WHITE));
        }
    }

    static Optional<float[]> anchor(StageFrame f, Card card, Anchor anchor, int index) {
        return switch (card) {
            case Card.Title c -> Optional.of(titleAnchor(f, c, anchor, index));
            case Card.Guess c -> Optional.of(StageKit.center(f));
            case Card.HoursReveal c -> {
                Hero h = hoursLayout(f, c);
                yield Optional.of(StageKit.heroBigCenter(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, c.hours())));
            }
            default -> Optional.empty();
        };
    }

    private static float[] titleAnchor(StageFrame f, Card.Title c, Anchor anchor, int index) {
        String year = Integer.toString(c.year());
        int scale = yearScale(f, year);
        Rect yr = titleLayout(f, year, scale).year();
        if (anchor != Anchor.DIGIT) {
            return new float[] {yr.x() + yr.w() / 2f, yr.y() + yr.h() / 2f};
        }
        int i = Math.max(0, Math.min(year.length() - 1, index));
        Font font = f.font();
        int x0 = yr.x() + font.width(year.substring(0, i)) * scale;
        int dw = font.width(year.substring(i, i + 1)) * scale;
        return new float[] {x0 + dw / 2f, yr.y() + yr.h()};
    }

    private static int yearScale(StageFrame f, String year) {
        return StageLayout.titleYearScale(f.w(), f.h(), f.font().width(year));
    }

    private static StageLayout.Title titleLayout(StageFrame f, String year, int scale) {
        return StageLayout.title(f.w(), f.h(), f.font().width(year), scale, f.view().hasPreviousYear(),
                f.view().hasNextYear());
    }

    private static void tab(StageFrame f, String text, Rect r, boolean active) {
        Font font = f.font();
        boolean hover = StageKit.hovered(f, r);
        Identifier sprite = active ? (hover ? TAB_SELECTED_HOVER : TAB_SELECTED) : (hover ? TAB_HOVER : TAB);
        f.g().blitSprite(RenderPipelines.GUI_TEXTURED, sprite, r.x(), r.y(), r.w(), r.h());
        String t = StageLayout.fitText(text, Math.max(1, r.w() - 6), font::width);
        int tx = r.x() + (r.w() - font.width(t)) / 2;
        int ty = r.y() + (r.h() - 8) / 2;
        f.g().text(font, t, tx, ty, active ? StageKit.WHITE : TAB_IDLE_TEXT, true);
        if (active) {
            f.g().fill(tx, ty + 9, tx + font.width(t), ty + 10, StageKit.WHITE);
        }
    }

    private static void outline(GuiGraphicsExtractor g, Rect r) {
        int x0 = r.x();
        int y0 = r.y();
        int x1 = r.x() + r.w();
        int y1 = r.y() + r.h();
        g.fill(x0, y0, x1, y0 + 2, PICK_OUTLINE);
        g.fill(x0, y1 - 2, x1, y1, PICK_OUTLINE);
        g.fill(x0, y0 + 2, x0 + 2, y1 - 2, PICK_OUTLINE);
        g.fill(x1 - 2, y0 + 2, x1, y1 - 2, PICK_OUTLINE);
    }

    private static List<String> hoursLines(StageFrame f, Card.HoursReveal c) {
        List<String> out = new ArrayList<>();
        out.add(c.relatable());
        c.changeText().ifPresent(out::add);
        f.view().guessPick().ifPresent(p -> out.add(guessLine(p, c.hours())));
        return out;
    }

    static String guessLine(long pick, long hours) {
        String guessed = "You guessed " + Numbers.grouped(pick);
        if (pick == hours) {
            return "Spot on! " + guessed;
        }
        if (Math.abs(pick - hours) * 10 <= Math.max(1, hours)) {
            return "So close! " + guessed;
        }
        return guessed;
    }

    private static Hero hoursLayout(StageFrame f, Card.HoursReveal c) {
        Rect content = StageKit.content(f);
        int ls = StageKit.labelScale(content);
        List<Part> parts = new ArrayList<>();
        parts.add(Part.text(HOURS_LABEL, ls));
        parts.add(Part.big(Numbers.grouped(c.hours()), HOURS_MAX_SCALE, 1));
        parts.add(Part.text(c.hours() == 1 ? "hour" : "hours", ls + 1));
        List<String> lines = hoursLines(f, c);
        if (!lines.isEmpty()) {
            parts.add(Part.box(DIVIDER_W, DIVIDER_H));
        }
        for (String line : lines) {
            parts.add(Part.text(line, 2));
        }
        return StageKit.hero(f, true, 0, parts);
    }
}
