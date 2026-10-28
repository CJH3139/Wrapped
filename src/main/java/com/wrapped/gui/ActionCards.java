package com.wrapped.gui;

import com.wrapped.cards.Card;
import com.wrapped.cards.PlayerType;
import com.wrapped.compat.Compat;
import com.wrapped.gui.StageKit.Hero;
import com.wrapped.gui.StageKit.Part;
import com.wrapped.gui.StageKit.Stamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ActionCards {
    static final String BLOCKS_LABEL = "Blocks";
    static final String MINED = "Mined";
    static final String PLACED = "Placed";
    static final String FIGHTS_LABEL = "Fights";
    static final String TYPE_LABEL = "Your player type";
    static final String SAVE = "Save as image";
    private static final long LAND_MS = 1900;
    private static final int COUNT_MAX_SCALE = 12;
    private static final long FIGHT_START_MS = 300;
    private static final long FIGHT_STEP_MS = 300;
    private static final long KILLER_AFTER_MS = 200;
    private static final int FIGHT_BOX_MAX_W = 180;
    private static final int FIGHT_VALUE_MAX = 4;
    private static final long TYPE_POP_MS = 500;
    private static final float TYPE_DEG = -3f;
    private static final int TYPE_MAX_SCALE = 10;
    private static final double TYPE_WIDTH_SHARE = 0.9;
    private static final double TYPE_HEIGHT_SHARE = 0.45;
    private static final int SUMMARY_HEADER_MAX = 2;
    private static final int SUMMARY_HERO_MAX = 8;
    private static final int SUMMARY_VALUE_MAX = 2;
    private static final int SUMMARY_CHART_MIN = 34;
    private static final int CHART_PAD = 5;
    private static final int CHART_GAP = 1;
    private static final int CHART_TOP = 3;
    private static final int CHART_COLD = 0xFFE0E0E0;
    private static final long CHART_START_MS = 250;
    private static final long CHART_STEP_MS = 25;
    private static final long CHART_RISE_MS = 300;
    private static final int BIG = 1;

    private record Stat(String label, String value, ItemStack icon) {
        Stat(String label, String value) {
            this(label, value, ItemStack.EMPTY);
        }
    }

    private record FightLayout(Hero hero, List<Stat> stats, List<Rect> boxes, int valueScale, int killerPart) {
    }

    private record SummaryLayout(Rect card, int headerY, int headerScale, int heroY, int heroScale, int subY,
            int subScale, List<Stat> stats, List<Rect> boxes, int valueScale, Optional<Rect> chart) {
    }

    private record SummaryKey(Card.Summary card, int w, int h, boolean hideTopWord, String header) {
    }

    private static SummaryKey summaryKey;
    private static SummaryLayout summaryCached;

    private ActionCards() {
    }

    static void blocks(StageFrame f, Card.Blocks c) {
        long t = StageKit.elapsed(f);
        Hero h = blocksLayout(f, c);
        StageKit.heroItem(f, h, new ItemStack(Items.DIAMOND_PICKAXE));
        StageKit.heroText(f, h, 0, BLOCKS_LABEL, StageKit.WHITE);
        long target = Math.max(c.mined(), c.placed());
        long cv = CardTiming.count(c).map(w -> CardTiming.countValue(w, t)).orElse(target);
        boolean minedLarger = c.mined() >= c.placed();
        long[] values = {share(c.mined(), cv, target), share(c.placed(), cv, target)};
        String[] captions = {MINED, PLACED};
        int s = h.scale()[2];
        for (int k = 0; k < 2; k++) {
            boolean larger = (k == 0) == minedLarger;
            String text = StageKit.heroNumber(f, h, BIG, values[k]);
            StageKit.heroBig(f, h, BIG, k, text, larger ? Ease.punch(t - LAND_MS) : 1f);
            int cellCx = h.cellX(BIG, k) + h.cellW(BIG) / 2;
            StageKit.centered(f, captions[k], cellCx, h.y()[2], h.cellW(BIG), s, StageKit.WHITE, true);
        }
    }

    static void fights(StageFrame f, Card.Fights c) {
        long t = StageKit.elapsed(f);
        FightLayout l = fightLayout(f, c);
        Hero h = l.hero();
        StageKit.heroItem(f, h, new ItemStack(Items.IRON_SWORD));
        StageKit.heroText(f, h, 0, FIGHTS_LABEL, StageKit.WHITE);
        List<Stat> stats = l.stats();
        for (int k = 0; k < stats.size(); k++) {
            long start = FIGHT_START_MS + k * FIGHT_STEP_MS;
            if (t < start) {
                continue;
            }
            Rect r = l.boxes().get(k);
            Stat st = stats.get(k);
            int vs = l.valueScale();
            String value = st.value();
            StageKit.inScale(f, r.x() + r.w() / 2f, r.y() + r.h() / 2f, Ease.pop(t - start),
                    () -> StageKit.box(f, r, st.label(), value, vs));
        }
        if (l.killerPart() >= 0) {
            long last = FIGHT_START_MS + (stats.size() - 1) * FIGHT_STEP_MS;
            int color = StageKit.faded(Ease.fade(t - (last + KILLER_AFTER_MS), StageKit.FADE_MS), StageKit.WHITE);
            StageKit.heroText(f, h, l.killerPart(), killerText(c), color);
        }
    }

    static void type(StageFrame f, Card.Type c) {
        long t = StageKit.elapsed(f);
        String name = typeName(c.playerType());
        Stamp st = typeStamp(f, name);
        Hero h = typeLayout(f, c, st);
        StageKit.heroText(f, h, 0, TYPE_LABEL, StageKit.WHITE);
        if (t >= TYPE_POP_MS) {
            StageKit.drawStamp(f, name, st, h.cx(), h.y()[1] + st.boundH() / 2, TYPE_DEG, StageKit.GOLD,
                    Ease.pop(t - TYPE_POP_MS));
        }
        StageKit.heroText(f, h, 2, blurb(c.playerType()),
                StageKit.faded(Ease.fade(t - TYPE_POP_MS, StageKit.FADE_MS), StageKit.WHITE));
    }

    static void summary(StageFrame f, Card.Summary c) {
        long t = StageKit.elapsed(f);
        SummaryLayout l = summaryLayout(f, c);
        Rect card = l.card();
        StageKit.inScale(f, card.x() + card.w() / 2f, card.y() + card.h() / 2f, Ease.pop(t),
                () -> drawSummary(f, c, l));
    }

    static Optional<float[]> anchor(StageFrame f, Card card, Anchor anchor, int index) {
        if (card instanceof Card.Blocks c) {
            Hero h = blocksLayout(f, c);
            int k = c.mined() >= c.placed() ? 0 : 1;
            long v = k == 0 ? c.mined() : c.placed();
            return Optional.of(StageKit.heroBigCenter(f, h, BIG, k, StageKit.heroNumber(f, h, BIG, v)));
        } else if (card instanceof Card.Fights c) {
            FightLayout l = fightLayout(f, c);
            if (l.boxes().isEmpty()) {
                return Optional.of(StageKit.center(f));
            }
            Rect r = l.boxes().get(Math.max(0, Math.min(l.boxes().size() - 1, index)));
            return Optional.of(new float[] {r.x() + r.w() / 2f, r.y() + r.h() / 2f});
        } else if (card instanceof Card.Type c) {
            String name = typeName(c.playerType());
            Stamp st = typeStamp(f, name);
            Hero h = typeLayout(f, c, st);
            return Optional.of(new float[] {h.cx(), h.y()[1] + st.boundH() / 2f});
        } else if (card instanceof Card.Summary c) {
            Rect r = StageLayout.shareCard(f.w(), f.h());
            return Optional.of(new float[] {r.x() + r.w() / 2f, r.y() + r.h() / 2f});
        }
        return Optional.empty();
    }

    static String typeName(PlayerType type) {
        StringBuilder out = new StringBuilder();
        for (String word : type.name().split("_")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    static String blurb(PlayerType type) {
        return switch (type) {
            case SERVER_HOPPER -> "Always off to a new server";
            case LOYAL_REGULAR -> "One server had your heart";
            case NIGHT_OWL -> "Your best hours came after dark";
            case CHATTERBOX -> "Chat was never quiet with you";
            case BUILDER -> "You placed more than you broke";
            case MINER -> "Always digging deeper";
            case EXPLORER -> "A bit of everything";
        };
    }

    private static long share(long value, long cv, long target) {
        if (target <= 0 || cv >= target) {
            return value;
        }
        return Math.min(value, (long) Math.floor((double) value * cv / target));
    }

    private static Hero blocksLayout(StageFrame f, Card.Blocks c) {
        int ls = StageKit.labelScale(StageKit.content(f));
        String mined = Numbers.grouped(c.mined());
        String placed = Numbers.grouped(c.placed());
        String measure = f.font().width(mined) >= f.font().width(placed) ? mined : placed;
        return StageKit.hero(f, true, 0, List.of(Part.text(BLOCKS_LABEL, ls), Part.big(measure, COUNT_MAX_SCALE, 2),
                Part.text(PLACED + "  " + MINED, 2)));
    }

    private static List<Stat> fightStats(Card.Fights c) {
        List<Stat> out = new ArrayList<>();
        out.add(new Stat("Deaths", Numbers.grouped(c.deaths())));
        c.heartsDealt().ifPresent(v -> out.add(new Stat("Hearts dealt", Numbers.grouped(v))));
        c.heartsTaken().ifPresent(v -> out.add(new Stat("Hearts taken", Numbers.grouped(v))));
        c.mobKills().ifPresent(v -> out.add(new Stat("Mob kills", Numbers.grouped(v))));
        c.playersKilled().ifPresent(v -> out.add(new Stat("Players killed", Numbers.grouped(v))));
        return out;
    }

    private static long[] fightRaw(Card.Fights c) {
        List<Long> out = new ArrayList<>();
        out.add(c.deaths());
        c.heartsDealt().ifPresent(out::add);
        c.heartsTaken().ifPresent(out::add);
        c.mobKills().ifPresent(out::add);
        c.playersKilled().ifPresent(out::add);
        return out.stream().mapToLong(Long::longValue).toArray();
    }

    private static String killerText(Card.Fights c) {
        return "Killed you most: " + c.topKiller().orElse("");
    }

    private static FightLayout fightLayout(StageFrame f, Card.Fights c) {
        Font font = f.font();
        Rect content = StageKit.content(f);
        int gap = StageKit.gap(content);
        boolean side = f.w() >= f.h();
        List<Stat> stats = fightStats(c);
        long[] raw = fightRaw(c);
        int n = stats.size();
        int cols = side ? Math.min(n, 3) : Math.min(n, 2);
        if (n == 4) {
            cols = 2;
        }
        int rows = (n + cols - 1) / cols;
        int colW = StageKit.columnWidth(f, true);
        int boxW = Math.max(16, Math.min(FIGHT_BOX_MAX_W, (colW - (cols - 1) * gap) / cols));
        int ls = StageKit.labelScale(content);
        int labelH = StageKit.LINE * ls;
        boolean killer = c.topKiller().isPresent();
        int killerH = killer ? StageKit.LINE * 2 + gap : 0;
        int room = content.h() - labelH - gap - killerH - (side ? 0 : StageKit.heroSize(content) + gap);
        int vs = FIGHT_VALUE_MAX;
        while (vs > 1 && rows * StageKit.boxHeight(vs) + (rows - 1) * gap > room) {
            vs--;
        }
        int inner = StageKit.boxInner(boxW);
        int widest = vs;
        for (int k = 0; k < n; k++) {
            widest = Math.min(widest, StageKit.textScale(font, stats.get(k).value(), inner, vs));
        }
        vs = Math.max(1, widest);
        int boxH = StageKit.boxHeight(vs);
        int gridW = cols * boxW + (cols - 1) * gap;
        int gridH = rows * boxH + (rows - 1) * gap;
        List<Part> parts = new ArrayList<>(List.of(Part.text(FIGHTS_LABEL, ls), Part.box(gridW, gridH)));
        if (killer) {
            parts.add(Part.text(killerText(c), 2));
        }
        Hero h = StageKit.hero(f, true, 0, parts);
        int gx = h.left() ? h.x() : h.cx() - gridW / 2;
        int gy = h.y()[1];
        List<Rect> boxes = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            int row = k / cols;
            int col = k % cols;
            int inRow = Math.min(cols, n - row * cols);
            int offset = h.left() ? 0 : (cols - inRow) * (boxW + gap) / 2;
            boxes.add(new Rect(gx + offset + col * (boxW + gap), gy + row * (boxH + gap), boxW, boxH));
        }
        List<Stat> fitted = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            fitted.add(new Stat(stats.get(k).label(), StageKit.number(font, raw[k], inner, vs)));
        }
        return new FightLayout(h, List.copyOf(fitted), List.copyOf(boxes), vs, killer ? 2 : -1);
    }

    private static Stamp typeStamp(StageFrame f, String name) {
        Rect content = StageKit.content(f);
        return StageKit.stamp(f.font(), name, (int) (content.w() * TYPE_WIDTH_SHARE), TYPE_MAX_SCALE, TYPE_DEG,
                (int) (content.h() * TYPE_HEIGHT_SHARE));
    }

    private static Hero typeLayout(StageFrame f, Card.Type c, Stamp st) {
        int ls = StageKit.labelScale(StageKit.content(f));
        return StageKit.hero(f, false, 0, List.of(Part.text(TYPE_LABEL, ls), Part.box(st.boundW(), st.boundH()),
                Part.text(blurb(c.playerType()), 2)));
    }

    private static List<Stat> summaryStats(StageFrame f, Card.Summary c) {
        Card.Summary.Extras e = c.extras();
        List<Stat> out = new ArrayList<>();
        out.add(new Stat("Player type", typeName(c.playerType()), new ItemStack(Items.NETHER_STAR)));
        if (c.longestStreakDays() >= 1) {
            out.add(new Stat("Best streak", c.longestStreakDays() + (c.longestStreakDays() == 1 ? " day" : " days"),
                    new ItemStack(Items.TORCH)));
        }
        if (e.servers() >= 2) {
            out.add(new Stat("Servers", Numbers.grouped(e.servers()), new ItemStack(Items.COMPASS)));
        }
        e.longestSession().filter(d -> d.toMinutes() >= 1).ifPresent(d -> out.add(
                new Stat("Longest session", Numbers.duration(d), Compat.redBed())));
        if (e.messagesSent() > 0) {
            out.add(new Stat("Messages sent", Numbers.grouped(e.messagesSent()), new ItemStack(Items.WRITABLE_BOOK)));
        }
        if (!f.view().hideTopWord()) {
            e.topWord().ifPresent(w -> out.add(
                    new Stat("Top word", w, new ItemStack(Items.NAME_TAG))));
        }
        e.blocksMined().stream().filter(n -> n > 0).forEach(n -> out.add(
                new Stat("Blocks mined", Numbers.grouped(n), new ItemStack(Items.DIAMOND_PICKAXE))));
        e.mobKills().stream().filter(n -> n > 0).forEach(n -> out.add(
                new Stat("Mob kills", Numbers.grouped(n), new ItemStack(Items.IRON_SWORD))));
        if (e.deaths() > 0) {
            out.add(new Stat("Deaths", Numbers.grouped(e.deaths()), new ItemStack(Items.SKELETON_SKULL)));
        }
        return out;
    }

    private static boolean hasChart(Card.Summary c) {
        for (long v : c.extras().secondsByHour()) {
            if (v > 0) {
                return true;
            }
        }
        return false;
    }

    private static int stackHeight(int rows, int boxH, int step, int chartH) {
        int h = rows * boxH + Math.max(0, rows - 1) * step;
        return chartH > 0 ? h + step + chartH : h;
    }

    private static SummaryLayout summaryLayout(StageFrame f, Card.Summary c) {
        SummaryKey key = new SummaryKey(c, f.w(), f.h(), f.view().hideTopWord(), header(f));
        if (!key.equals(summaryKey)) {
            summaryCached = computeSummaryLayout(f, c);
            summaryKey = key;
        }
        return summaryCached;
    }

    private static SummaryLayout computeSummaryLayout(StageFrame f, Card.Summary c) {
        Font font = f.font();
        Rect card = StageLayout.shareCard(f.w(), f.h());
        int pad = Math.max(6, card.w() / 16);
        int gap = Math.max(4, card.h() / 50);
        int iw = Math.max(1, card.w() - 2 * pad);
        int headerScale = StageKit.textScale(font, header(f), iw, SUMMARY_HEADER_MAX);
        int headerY = card.y() + pad;
        int heroMax = Math.max(1, Math.min(SUMMARY_HERO_MAX, card.h() / 6 / StageKit.LINE));
        int heroScale = StageKit.textScale(font, Numbers.grouped(c.totalHours()), iw, heroMax);
        int heroY = headerY + StageKit.LINE * headerScale + gap;
        int subScale = StageKit.textScale(font, "hours played", iw, card.w() >= 200 ? 2 : 1);
        int subY = heroY + StageKit.LINE * heroScale + 2;
        int top = subY + StageKit.LINE * subScale + gap;
        int room = StageLayout.shareSave(card).y() - gap - top;
        Optional<Stat> wide = c.topServer().map(s -> new Stat("Top server", s, new ItemStack(Items.GOLD_BLOCK)));
        List<Stat> grid = new ArrayList<>(summaryStats(f, c));
        int half = (iw - gap) / 2;
        boolean pair = true;
        for (Stat s : grid) {
            int inner = StageKit.boxInner(half, true);
            if (font.width(s.value()) > inner || font.width(s.label()) > inner) {
                pair = false;
            }
        }
        int chartH = hasChart(c) ? Math.max(SUMMARY_CHART_MIN, card.h() / 7) : 0;
        int vs;
        int rows;
        while (true) {
            rows = (wide.isPresent() ? 1 : 0) + (pair ? (grid.size() + 1) / 2 : grid.size());
            vs = SUMMARY_VALUE_MAX;
            while (vs > 1 && stackHeight(rows, StageKit.boxHeight(vs), gap, chartH) > room) {
                vs--;
            }
            if (stackHeight(rows, StageKit.boxHeight(vs), gap, chartH) <= room || grid.size() <= 1 && chartH == 0) {
                break;
            }
            if (chartH > 0) {
                chartH = 0;
            } else {
                grid.remove(grid.size() - 1);
            }
        }
        if (wide.isPresent()) {
            vs = Math.min(vs, StageKit.textScale(font, wide.get().value(), StageKit.boxInner(iw, true), vs));
        }
        for (Stat s : grid) {
            vs = Math.min(vs, StageKit.textScale(font, s.value(), StageKit.boxInner(pair ? half : iw, true), vs));
        }
        vs = Math.max(1, vs);
        int boxH = StageKit.boxHeight(vs);
        int spaces = Math.max(0, rows - 1) + (chartH > 0 ? 1 : 0);
        int extra = Math.max(0, room - stackHeight(rows, boxH, gap, chartH));
        int step = gap + (spaces > 0 ? Math.min(boxH / 2, extra / (spaces + 2)) : 0);
        int y = top + Math.max(0, (room - stackHeight(rows, boxH, step, chartH)) / 2);
        int x = card.x() + pad;
        List<Stat> stats = new ArrayList<>();
        List<Rect> boxes = new ArrayList<>();
        if (wide.isPresent()) {
            stats.add(wide.get());
            boxes.add(new Rect(x, y, iw, boxH));
            y += boxH + step;
        }
        for (int k = 0; k < grid.size(); k++) {
            stats.add(grid.get(k));
            if (pair) {
                boolean right = k % 2 == 1;
                boolean alone = !right && k == grid.size() - 1;
                boxes.add(new Rect(right ? x + iw - half : x, y, alone ? iw : half, boxH));
                if (right || alone) {
                    y += boxH + step;
                }
            } else {
                boxes.add(new Rect(x, y, iw, boxH));
                y += boxH + step;
            }
        }
        Optional<Rect> chart = chartH > 0 ? Optional.of(new Rect(x, y, iw, chartH)) : Optional.empty();
        return new SummaryLayout(card, headerY, headerScale, heroY, heroScale, subY, subScale, List.copyOf(stats),
                List.copyOf(boxes), vs, chart);
    }

    private static String header(StageFrame f) {
        return "WRAPPED " + f.view().year();
    }

    private static void drawSummary(StageFrame f, Card.Summary c, SummaryLayout l) {
        Font font = f.font();
        Rect card = l.card();
        Panels.panel(f.g(), card.x(), card.y(), card.w(), card.h());
        int pad = Math.max(6, card.w() / 16);
        int iw = Math.max(1, card.w() - 2 * pad);
        int cx = card.x() + card.w() / 2;
        StageKit.centered(f, header(f), cx, l.headerY(), iw, l.headerScale(), StageKit.INK, false);
        String hours = StageKit.number(font, c.totalHours(), iw, l.heroScale());
        StageDraw.giant(f, hours, cx, l.heroY(), iw, l.heroScale(), 1f);
        StageKit.centered(f, "hours played", cx, l.subY(), iw, l.subScale(), StageKit.INK, false);
        for (int k = 0; k < Math.min(l.stats().size(), l.boxes().size()); k++) {
            Stat st = l.stats().get(k);
            StageKit.box(f, l.boxes().get(k), st.label(), st.value(), l.valueScale(), st.icon());
        }
        l.chart().ifPresent(r -> chart(f, c, r));
        if (StageKit.live(f)) {
            CardDraw.button(f.g(), font, SAVE, StageLayout.shareSave(card), f.mx(), f.my());
        }
    }

    private static void chart(StageFrame f, Card.Summary c, Rect r) {
        long t = StageKit.elapsed(f);
        Font font = f.font();
        long[] secs = c.extras().secondsByHour();
        StageDraw.slot(f, r);
        int inner = StageKit.boxInner(r.w());
        String label = StageLayout.fitText(HabitCards.PLAY_LABEL + ": " + Card.PlayTime.label(secs), inner,
                font::width);
        f.g().text(font, label, r.x() + CHART_PAD, r.y() + CHART_PAD, StageKit.SLOT_LABEL, true);
        int n = secs.length;
        int barW = Math.max(1, (inner - (n - 1) * CHART_GAP) / n);
        int total = n * barW + (n - 1) * CHART_GAP;
        int x0 = r.x() + (r.w() - total) / 2;
        int base = r.y() + r.h() - CHART_PAD;
        int maxH = Math.max(1, base - (r.y() + CHART_PAD + StageKit.LINE + CHART_TOP));
        long max = 1;
        for (long v : secs) {
            max = Math.max(max, v);
        }
        for (int i = 0; i < n; i++) {
            if (secs[i] <= 0) {
                continue;
            }
            float rise = Ease.fade(t - CHART_START_MS - i * CHART_STEP_MS, CHART_RISE_MS);
            int bh = Math.max(1, Math.round(maxH * (float) secs[i] / max * rise));
            int bx = x0 + i * (barW + CHART_GAP);
            f.g().fill(bx, base - bh, bx + barW, base, Card.PlayTime.inPeak(secs, i) ? StageKit.GOLD : CHART_COLD);
        }
    }
}
