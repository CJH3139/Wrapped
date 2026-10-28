package com.wrapped.gui;

import com.wrapped.cards.Card;
import com.wrapped.compat.Compat;
import com.wrapped.gui.StageKit.Hero;
import com.wrapped.gui.StageKit.Part;
import com.wrapped.stats.ServerStat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class PlaceCards {
    static final String SERVERS_LABEL = "Servers visited";
    static final String SERVERS_SUB = "different servers this year";
    static final String COUNTDOWN_LABEL = "Your top servers";
    static final String TOP_LABEL = "Number one";
    static final String SESSION_LABEL = "Longest session";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ROOT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final long LAND_MS = 1900;
    private static final int COUNT_MAX_SCALE = 12;
    private static final int RANK_MAX_SCALE = 3;
    private static final int HOST_MAX_SCALE = 10;
    private static final double HOST_SHARE = 0.96;
    private static final long SLIDE_MS = 450;
    private static final long FILL_MS = 900;
    private static final int FILL = 0xFF6FBF3A;
    private static final int FILL_TOP = 0xFF9BE05E;
    private static final int FILL_BOTTOM = 0xFF4A8A24;
    private static final int BAR_INSET = 2;
    private static final long TOP_POP_MS = 250;
    private static final long STAT_STEP_MS = 100;
    private static final int STAT_MAX_W = 200;
    private static final int BIG = 1;

    private record TopLayout(int labelY, int hostY, int hostScale, int hostW, List<Rect> boxes, int valueScale) {
    }

    private PlaceCards() {
    }

    static void serverCount(StageFrame f, Card.ServerCount c) {
        long t = StageKit.elapsed(f);
        Hero h = serverLayout(f, c);
        StageKit.heroItem(f, h, new ItemStack(Items.COMPASS));
        StageKit.heroText(f, h, 0, SERVERS_LABEL, StageKit.WHITE);
        long value = CardTiming.count(c).map(w -> CardTiming.countValue(w, t)).orElse((long) c.count());
        StageKit.heroBig(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, value), Ease.punch(t - LAND_MS));
        StageKit.heroText(f, h, 2, SERVERS_SUB, StageKit.WHITE);
    }

    static void countdown(StageFrame f, Card.Countdown c) {
        Font font = f.font();
        GuiGraphicsExtractor g = f.g();
        Rect content = StageKit.content(f);
        List<ServerStat> ranked = c.ranked();
        int n = ranked.size();
        int cx = content.x() + content.w() / 2;
        if (n == 0) {
            int labelH = StageKit.LINE * StageKit.labelScale(content);
            StageDraw.label(f, COUNTDOWN_LABEL, cx, content.y() + (content.h() - labelH) / 2);
            return;
        }
        int dy = countdownShift(f, n);
        StageDraw.label(f, COUNTDOWN_LABEL, cx, content.y() + dy);
        long topTime = 1;
        for (ServerStat s : ranked) {
            topTime = Math.max(topTime, s.time().toMillis());
        }
        int rowH = StageLayout.countdownRow(f.w(), f.h(), 0, n).h();
        int rankScale = Math.max(1, Math.min(RANK_MAX_SCALE, rowH / StageKit.LINE));
        int rankW = rankWidth(font, n, rankScale);
        int gap = StageKit.gap(content);
        int textScale = rowH >= 30 && content.w() >= 360 ? 2 : 1;
        int pad = Math.max(4, rowH / 4);
        int step = f.view().step();
        g.enableScissor(content.x(), content.y(), content.x() + content.w(), content.y() + content.h());
        for (int i = 0; i < n; i++) {
            if (step < i) {
                continue;
            }
            ServerStat s = ranked.get(i);
            Rect base = StageLayout.countdownRow(f.w(), f.h(), n - 1 - i, n);
            long se = i == step ? StageKit.stepElapsed(f) : StageKit.REST;
            int dx = Math.round(Ease.slide(se) * base.w());
            Rect r = new Rect(base.x() + dx, base.y() + dy, base.w(), base.h());
            String rank = "#" + (n + 1 - i);
            StageDraw.giant(f, rank, r.x() + rankW / 2, r.y() + (r.h() - StageKit.LINE * rankScale) / 2, rankW,
                    rankScale, 1f);
            Rect bar = new Rect(r.x() + rankW + gap, r.y(), Math.max(8, r.w() - rankW - gap), r.h());
            StageDraw.slot(f, bar);
            float grow = fillProgress(se);
            double share = Math.min(1.0, (double) s.time().toMillis() / topTime);
            int innerW = bar.w() - 2 * BAR_INSET;
            int fillW = (int) Math.round(innerW * share * grow);
            if (fillW > 0) {
                int fx = bar.x() + BAR_INSET;
                int fy0 = bar.y() + BAR_INSET;
                int fy1 = bar.y() + bar.h() - BAR_INSET;
                int band = Math.max(1, (fy1 - fy0) / 7);
                g.fill(fx, fy0, fx + fillW, fy1, FILL);
                g.fill(fx, fy0, fx + fillW, fy0 + band, FILL_TOP);
                g.fill(fx, fy1 - band, fx + fillW, fy1, FILL_BOTTOM);
            }
            String hours = Numbers.hours(s.time());
            int hoursW = font.width(hours) * textScale;
            int ty = bar.y() + (bar.h() - StageKit.LINE * textScale) / 2 + 1;
            int hostAvail = Math.max(1, (bar.w() - 3 * pad - hoursW) / textScale);
            String host = StageLayout.fitText(s.host(), hostAvail, font::width);
            StageKit.scaled(f, host, bar.x() + pad, ty, textScale, StageKit.WHITE, true);
            StageKit.scaled(f, hours, bar.x() + bar.w() - pad - hoursW, ty, textScale, StageKit.WHITE, true);
        }
        g.disableScissor();
    }

    static void topServer(StageFrame f, Card.TopServer c) {
        long t = StageKit.elapsed(f);
        Rect content = StageKit.content(f);
        TopLayout l = topLayout(f, c);
        int cx = content.x() + content.w() / 2;
        StageDraw.label(f, TOP_LABEL, cx, l.labelY());
        if (t >= TOP_POP_MS) {
            StageDraw.giant(f, c.server().host(), cx, l.hostY(), l.hostW(), l.hostScale(),
                    Ease.pop(t - TOP_POP_MS));
        }
        String[] labels = {"Hours", "Joins", "Days", "Since"};
        String[] values = topValues(f.font(), c.server(), l);
        for (int k = 0; k < l.boxes().size(); k++) {
            long start = TOP_POP_MS + k * STAT_STEP_MS;
            if (t < start) {
                continue;
            }
            Rect r = l.boxes().get(k);
            String label = labels[k];
            String value = values[k];
            StageKit.inScale(f, r.x() + r.w() / 2f, r.y() + r.h() / 2f, Ease.pop(t - start),
                    () -> StageKit.box(f, r, label, value, l.valueScale()));
        }
    }

    static void longestSession(StageFrame f, Card.LongestSession c) {
        long t = StageKit.elapsed(f);
        Hero h = sessionLayout(f, c);
        StageKit.heroItem(f, h, Compat.redBed());
        StageKit.heroText(f, h, 0, SESSION_LABEL, StageKit.WHITE);
        long minutes = CardTiming.count(c).map(w -> CardTiming.countValue(w, t)).orElse(c.duration().toMinutes());
        String text = Numbers.duration(Duration.ofMinutes(minutes));
        StageKit.heroBig(f, h, BIG, 0, text, Ease.punch(t - LAND_MS));
        int line = StageKit.faded(Ease.fade(t - LAND_MS, StageKit.FADE_MS), StageKit.WHITE);
        StageKit.heroText(f, h, 2, c.serverName(), line);
        StageKit.heroText(f, h, 3, endedText(c), line);
    }

    static Optional<float[]> anchor(StageFrame f, Card card, Anchor anchor, int index) {
        return switch (card) {
            case Card.ServerCount c -> {
                Hero h = serverLayout(f, c);
                yield Optional.of(StageKit.heroBigCenter(f, h, BIG, 0, StageKit.heroNumber(f, h, BIG, c.count())));
            }
            case Card.Countdown c -> Optional.of(countdownAnchor(f, c, index));
            case Card.TopServer c -> Optional.of(topAnchor(f, c, anchor));
            case Card.LongestSession c -> {
                Hero h = sessionLayout(f, c);
                yield Optional.of(StageKit.heroBigCenter(f, h, BIG, 0, Numbers.duration(c.duration())));
            }
            default -> Optional.empty();
        };
    }

    private static float[] countdownAnchor(StageFrame f, Card.Countdown c, int index) {
        int n = c.ranked().size();
        if (n == 0) {
            return StageKit.center(f);
        }
        int i = Math.max(0, Math.min(n - 1, index));
        Rect r = StageLayout.countdownRow(f.w(), f.h(), n - 1 - i, n);
        int dy = countdownShift(f, n);
        int rankScale = Math.max(1, Math.min(RANK_MAX_SCALE, r.h() / StageKit.LINE));
        int rankW = rankWidth(f.font(), n, rankScale);
        return new float[] {r.x() + rankW / 2f, r.y() + dy + r.h() / 2f};
    }

    private static float[] topAnchor(StageFrame f, Card.TopServer c, Anchor anchor) {
        Rect content = StageKit.content(f);
        return switch (anchor) {
            case LEFT -> new float[] {content.x() + content.w() * 0.2f, content.y() + content.h() * 0.3f};
            case RIGHT -> new float[] {content.x() + content.w() * 0.8f, content.y() + content.h() * 0.35f};
            default -> {
                TopLayout l = topLayout(f, c);
                yield new float[] {content.x() + content.w() / 2f,
                        l.hostY() + StageKit.LINE * l.hostScale() / 2f};
            }
        };
    }

    private static int countdownShift(StageFrame f, int n) {
        Rect content = StageKit.content(f);
        Rect last = StageLayout.countdownRow(f.w(), f.h(), n - 1, n);
        int blockH = last.y() + last.h() - content.y();
        return Math.max(0, (content.h() - blockH) / 2);
    }

    private static int rankWidth(Font font, int n, int scale) {
        int w = 0;
        for (int i = 0; i < n; i++) {
            w = Math.max(w, font.width("#" + (n + 1 - i)) * scale);
        }
        return w + 2;
    }

    private static float fillProgress(long stepElapsed) {
        if (stepElapsed <= SLIDE_MS) {
            return 0f;
        }
        double k = Math.min(1.0, (double) (stepElapsed - SLIDE_MS) / FILL_MS);
        double inv = 1 - k;
        return (float) (1 - inv * inv * inv);
    }

    private static TopLayout topLayout(StageFrame f, Card.TopServer c) {
        Font font = f.font();
        Rect content = StageKit.content(f);
        int gap = StageKit.gap(content);
        boolean grid = f.h() > f.w();
        int cols = grid ? 2 : 4;
        int rows = grid ? 2 : 1;
        int boxW = Math.max(16, Math.min(STAT_MAX_W, (content.w() - (cols - 1) * gap) / cols));
        int maxValueScale = content.h() >= 300 ? 3 : 2;
        ServerStat s = c.server();
        String[] raw = {Numbers.hours(s.time()), Numbers.grouped(s.joins()), Numbers.grouped(s.activeDays()),
                DAY.format(s.firstVisit())};
        int vs = maxValueScale;
        for (String v : raw) {
            vs = Math.min(vs, StageKit.textScale(font, v, StageKit.boxInner(boxW), maxValueScale));
        }
        int boxH = StageKit.boxHeight(vs);
        int gridH = rows * boxH + (rows - 1) * gap;
        int gridW = cols * boxW + (cols - 1) * gap;
        int labelH = StageKit.LINE * StageKit.labelScale(content);
        int hostW = (int) (content.w() * HOST_SHARE);
        int hostRoom = content.h() - labelH - gridH - 3 * gap;
        int hostMax = Math.max(1, Math.min(HOST_MAX_SCALE, hostRoom / StageKit.LINE));
        int hs = StageKit.textScale(font, s.host(), hostW, hostMax);
        int hostH = StageKit.LINE * hs;
        int total = labelH + gap + hostH + 2 * gap + gridH;
        int top = content.y() + Math.max(0, (content.h() - total) / 2);
        int hostY = top + labelH + gap;
        int gridY = hostY + hostH + 2 * gap;
        int gx = content.x() + (content.w() - gridW) / 2;
        List<Rect> boxes = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            int col = k % cols;
            int row = k / cols;
            boxes.add(new Rect(gx + col * (boxW + gap), gridY + row * (boxH + gap), boxW, boxH));
        }
        return new TopLayout(top, hostY, hs, hostW, List.copyOf(boxes), vs);
    }

    private static String[] topValues(Font font, ServerStat s, TopLayout l) {
        int inner = StageKit.boxInner(l.boxes().get(0).w());
        int vs = l.valueScale();
        return new String[] {Numbers.hours(s.time()), StageKit.number(font, s.joins(), inner, vs),
                StageKit.number(font, s.activeDays(), inner, vs), DAY.format(s.firstVisit())};
    }

    private static String endedText(Card.LongestSession c) {
        return "ended " + DAY.format(c.end()) + " at " + TIME.format(c.end());
    }

    private static Hero serverLayout(StageFrame f, Card.ServerCount c) {
        int ls = StageKit.labelScale(StageKit.content(f));
        return StageKit.hero(f, true, 0, List.of(Part.text(SERVERS_LABEL, ls),
                Part.big(Numbers.grouped(c.count()), COUNT_MAX_SCALE, 1), Part.text(SERVERS_SUB, 2)));
    }

    private static Hero sessionLayout(StageFrame f, Card.LongestSession c) {
        int ls = StageKit.labelScale(StageKit.content(f));
        long target = Math.max(0, c.duration().toMinutes());
        String last = Numbers.duration(c.duration());
        String widest = target >= 60 ? (target >= 600 ? "99h 59m" : "9h 59m") : "59m";
        String measure = f.font().width(widest) > f.font().width(last) ? widest : last;
        return StageKit.hero(f, true, 0, List.of(Part.text(SESSION_LABEL, ls),
                Part.big(measure, COUNT_MAX_SCALE, 1), Part.text(c.serverName(), 2), Part.text(endedText(c), 2)));
    }
}
