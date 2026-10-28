package com.wrapped.gui;

import com.wrapped.cards.Card;
import com.wrapped.compat.Gfx;
import com.wrapped.gui.StageKit.Hero;
import com.wrapped.gui.StageKit.Part;
import com.wrapped.gui.StageKit.Stamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class HabitCards {
    static final String PLAY_LABEL = "When you play";
    static final String STREAK_LABEL = "Longest streak";
    static final String CHAT_LABEL = "Messages sent";
    static final String WORD_LABEL = "Most used word";
    static final String HIDDEN = "Hidden";
    static final String HIDE_WORD = "Hide word";
    static final String SHOW_WORD = "Show word";
    private static final String[] AXIS = {"12 AM", "6 AM", "12 PM", "6 PM", "11 PM"};
    private static final int[] AXIS_HOUR = {0, 6, 12, 18, 23};
    private static final int HOURS = 24;
    private static final int BAR_GAP = 2;
    private static final double BAR_SHARE = 0.4;
    private static final long BAR_START_MS = 200;
    private static final long BAR_STEP_MS = 45;
    private static final int BAR_COLD = 0x8CFFFFFF;
    private static final int BAR_SHADOW = 0x40000000;
    private static final int BAR_STUB = 2;
    private static final long PLAY_STAMP_MS = 1600;
    private static final float PLAY_STAMP_DEG = -5f;
    private static final int PLAY_STAMP_MAX = 6;
    private static final double STAMP_WIDTH_SHARE = 0.85;
    private static final long LAND_MS = 1900;
    private static final int COUNT_MAX_SCALE = 12;
    private static final int TORCH = 16;
    private static final int TORCH_GAP = 4;
    private static final int TORCH_MAX = 14;
    private static final long TORCH_START_MS = 400;
    private static final long TORCH_SPAN_MS = 1500;
    private static final long WORD_STAMP_MS = 2300;
    private static final float WORD_STAMP_DEG = -4f;
    private static final int WORD_STAMP_MAX = 5;
    private static final int BUTTON_H = 16;
    private static final int BIG = 1;

    private record PlayLayout(int labelY, int barX, int barW, int base, int barMax, int axisY, int axisScale,
            Stamp stamp, int stampCy) {
    }

    private HabitCards() {
    }

    static void playTime(StageFrame f, Card.PlayTime c) {
        long t = StageKit.elapsed(f);
        Font font = f.font();
        Gfx g = f.g();
        Rect content = StageKit.content(f);
        int cx = content.x() + content.w() / 2;
        PlayLayout l = playLayout(f, c);
        StageDraw.label(f, PLAY_LABEL, cx, l.labelY());
        long[] hours = c.secondsByHour();
        long max = 0;
        for (long v : hours) {
            max = Math.max(max, v);
        }
        int n = Math.min(HOURS, hours.length);
        for (int i = 0; i < n; i++) {
            long start = BAR_START_MS + i * BAR_STEP_MS;
            if (t < start) {
                continue;
            }
            double share = max <= 0 ? 0 : (double) Math.max(0, hours[i]) / max;
            int full = Math.max(BAR_STUB, (int) Math.round(l.barMax() * share));
            int bh = Math.max(1, Math.round(full * Ease.pop(t - start)));
            int x0 = l.barX() + i * (l.barW() + BAR_GAP);
            int x1 = x0 + l.barW();
            g.fill(x0 + 1, l.base() - bh + 1, x1 + 1, l.base() + 1, BAR_SHADOW);
            g.fill(x0, l.base() - bh, x1, l.base(), Card.PlayTime.inPeak(hours, i) ? StageKit.GOLD : BAR_COLD);
        }
        int chartW = HOURS * l.barW() + (HOURS - 1) * BAR_GAP;
        int left = l.barX();
        int right = l.barX() + chartW;
        for (int k = 0; k < AXIS.length; k++) {
            String text = AXIS[k];
            int tw = font.width(text) * l.axisScale();
            int x;
            if (k == 0) {
                x = left;
            } else if (k == AXIS.length - 1) {
                x = right - tw;
            } else {
                int barCx = l.barX() + AXIS_HOUR[k] * (l.barW() + BAR_GAP) + l.barW() / 2;
                x = Math.max(left, Math.min(right - tw, barCx - tw / 2));
            }
            StageKit.scaled(f, text, x, l.axisY(), l.axisScale(), StageKit.WHITE, true);
        }
        if (t >= PLAY_STAMP_MS) {
            StageKit.drawStamp(f, c.label(), l.stamp(), cx, l.stampCy(), PLAY_STAMP_DEG, StageKit.GOLD,
                    Ease.pop(t - PLAY_STAMP_MS));
        }
    }

    static void streak(StageFrame f, Card.Streak c) {
        long t = StageKit.elapsed(f);
        Hero h = streakLayout(f, c);
        StageKit.heroText(f, h, 0, STREAK_LABEL, StageKit.WHITE);
        long value = CardTiming.count(c).map(w -> CardTiming.countValue(w, t)).orElse((long) c.days());
        StageKit.heroBig(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, value), Ease.punch(t - LAND_MS));
        StageKit.heroText(f, h, 2, streakSub(c), StageKit.WHITE);
        int count = torchCount(f, c);
        if (count <= 0) {
            return;
        }
        int rowW = count * (TORCH + TORCH_GAP) - TORCH_GAP;
        int x0 = h.left() ? h.x() : h.cx() - rowW / 2;
        int y = h.y()[3];
        long spacing = TORCH_SPAN_MS / count;
        ItemStack torch = new ItemStack(Items.TORCH);
        for (int i = 0; i < count; i++) {
            long lit = TORCH_START_MS + i * spacing;
            if (t < lit) {
                continue;
            }
            int size = Math.max(1, Math.round(TORCH * Ease.pop(t - lit)));
            StageDraw.item(f, torch, x0 + i * (TORCH + TORCH_GAP) + TORCH / 2, y + TORCH / 2, size,
                    f.nowMs() + i * 170L);
        }
    }

    static void chat(StageFrame f, Card.ChatCard c) {
        long t = StageKit.elapsed(f);
        Hero h = chatLayout(f, c);
        StageKit.heroItem(f, h, new ItemStack(Items.WRITABLE_BOOK));
        StageKit.heroText(f, h, 0, CHAT_LABEL, StageKit.WHITE);
        long value = CardTiming.count(c).map(w -> CardTiming.countValue(w, t)).orElse((long) c.messagesSent());
        StageKit.heroBig(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, value), Ease.punch(t - LAND_MS));
        if (c.topWord().isEmpty()) {
            return;
        }
        StageKit.heroText(f, h, 2, WORD_LABEL, StageKit.faded(Ease.fade(t - LAND_MS, StageKit.FADE_MS),
                StageKit.WHITE));
        if (t >= WORD_STAMP_MS) {
            float[] at = stampCenter(f, h, c);
            StageKit.drawStamp(f, wordText(f, c), wordStamp(f, c), Math.round(at[0]), Math.round(at[1]),
                    WORD_STAMP_DEG, StageKit.WHITE, Ease.pop(t - WORD_STAMP_MS));
        }
        if (StageKit.live(f)) {
            CardDraw.button(f.g(), f.font(), f.view().hideTopWord() ? SHOW_WORD : HIDE_WORD,
                    StageLayout.hideWord(f.w(), f.h()), f.mx(), f.my());
        }
    }

    static Optional<float[]> anchor(StageFrame f, Card card, Anchor anchor, int index) {
        if (card instanceof Card.PlayTime c) {
            Rect content = StageKit.content(f);
            return Optional.of(new float[] {content.x() + content.w() / 2f, playLayout(f, c).stampCy()});
        } else if (card instanceof Card.Streak c) {
            Hero h = streakLayout(f, c);
            return Optional.of(StageKit.heroBigCenter(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, c.days())));
        } else if (card instanceof Card.ChatCard c) {
            Hero h = chatLayout(f, c);
            if (anchor == Anchor.STAMP && c.topWord().isPresent()) {
                return Optional.of(stampCenter(f, h, c));
            }
            return Optional.of(StageKit.heroBigCenter(f, h, BIG, 0,
                    StageKit.heroNumber(f, h, BIG, c.messagesSent())));
        }
        return Optional.empty();
    }

    private static PlayLayout playLayout(StageFrame f, Card.PlayTime c) {
        Font font = f.font();
        Rect content = StageKit.content(f);
        int gap = StageKit.gap(content);
        int labelH = StageKit.LINE * StageKit.labelScale(content);
        int axisScale = content.w() >= 480 ? 2 : 1;
        int axisH = StageKit.LINE * axisScale;
        int barMax = (int) (content.h() * BAR_SHARE);
        int barW = Math.max(1, (content.w() - (HOURS - 1) * BAR_GAP) / HOURS);
        int chartW = HOURS * barW + (HOURS - 1) * BAR_GAP;
        int stampRoom = content.h() - labelH - barMax - axisH - 4 * gap - 2;
        Stamp stamp = StageKit.stamp(font, c.label(), (int) (content.w() * STAMP_WIDTH_SHARE), PLAY_STAMP_MAX,
                PLAY_STAMP_DEG, Math.max(1, stampRoom));
        int total = labelH + gap + barMax + 2 + axisH + 2 * gap + stamp.boundH();
        int top = content.y() + Math.max(0, (content.h() - total) / 2);
        int base = top + labelH + gap + barMax;
        int axisY = base + 2;
        int stampCy = axisY + axisH + 2 * gap + stamp.boundH() / 2;
        return new PlayLayout(top, content.x() + (content.w() - chartW) / 2, barW, base, barMax, axisY, axisScale,
                stamp, stampCy);
    }

    private static String streakSub(Card.Streak c) {
        return c.days() == 1 ? "day in a row" : "days in a row";
    }

    private static int torchCount(StageFrame f, Card.Streak c) {
        int fit = (StageKit.content(f).w() + TORCH_GAP) / (TORCH + TORCH_GAP);
        return Math.max(0, Math.min(Math.min(c.days(), TORCH_MAX), fit));
    }

    private static Hero streakLayout(StageFrame f, Card.Streak c) {
        int ls = StageKit.labelScale(StageKit.content(f));
        int count = torchCount(f, c);
        List<Part> parts = new ArrayList<>(List.of(Part.text(STREAK_LABEL, ls),
                Part.big(Numbers.grouped(c.days()), COUNT_MAX_SCALE, 1), Part.text(streakSub(c), 2)));
        if (count > 0) {
            parts.add(Part.box(count * (TORCH + TORCH_GAP) - TORCH_GAP, TORCH));
        }
        return StageKit.hero(f, false, 0, parts);
    }

    private static String wordText(StageFrame f, Card.ChatCard c) {
        return f.view().hideTopWord() ? HIDDEN : c.topWord().map(HabitCards::quoted).orElse(HIDDEN);
    }

    private static String quoted(String word) {
        return "\"" + word + "\"";
    }

    private static Stamp wordStamp(StageFrame f, Card.ChatCard c) {
        Rect content = StageKit.content(f);
        int colW = StageKit.columnWidth(f, true);
        return StageKit.stamp(f.font(), wordText(f, c), (int) (colW * STAMP_WIDTH_SHARE), WORD_STAMP_MAX,
                WORD_STAMP_DEG, Math.max(1, content.h() / 4));
    }

    private static float[] stampCenter(StageFrame f, Hero h, Card.ChatCard c) {
        Stamp st = wordStamp(f, c);
        int x = h.left() ? h.x() + st.boundW() / 2 : h.cx();
        return new float[] {x, h.y()[3] + st.boundH() / 2f};
    }

    private static Hero chatLayout(StageFrame f, Card.ChatCard c) {
        Rect content = StageKit.content(f);
        int ls = StageKit.labelScale(content);
        List<Part> parts = new ArrayList<>(List.of(Part.text(CHAT_LABEL, ls),
                Part.big(Numbers.grouped(c.messagesSent()), COUNT_MAX_SCALE, 1)));
        int reserve = 0;
        if (c.topWord().isPresent()) {
            Stamp st = wordStamp(f, c);
            parts.add(Part.text(WORD_LABEL, 2));
            parts.add(Part.box(st.boundW(), st.boundH()));
            reserve = BUTTON_H + StageKit.gap(content);
        }
        return StageKit.hero(f, true, reserve, parts);
    }
}
