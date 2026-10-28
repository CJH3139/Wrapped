package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.cards.Card;
import com.wrapped.cards.PlayerType;
import com.wrapped.gui.CardTiming.Cue;
import com.wrapped.stats.ServerStat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StageControllerTest {
    private static ServerStat server(String host, long hours) {
        return new ServerStat(host, Duration.ofHours(hours), 5, 10, LocalDate.of(2026, 1, 1));
    }

    private static final Card TITLE = new Card.Title(2026);
    private static final Card GUESS = new Card.Guess(1218, List.of(400, 1200, 3000));
    private static final Card HOURS = new Card.HoursReveal(1218, "a lot", Optional.of(12), 2025);
    private static final Card COUNTDOWN3 = new Card.Countdown(List.of(server("c.net", 30), server("b.net", 50),
            server("a.net", 100)));
    private static final Card COUNTDOWN1 = new Card.Countdown(List.of(server("a.net", 100)));
    private static final Card SUMMARY = new Card.Summary(1218, Optional.of("a.net"), 9, PlayerType.EXPLORER);

    private static List<Card> deck(Card countdown) {
        return List.of(TITLE, GUESS, HOURS, countdown, SUMMARY);
    }

    private static long arrive(StageController c, int index, long t) {
        long now = t;
        while (c.index() != index) {
            c.next(now);
            c.update(now);
            now++;
        }
        return now - 1;
    }

    @Test
    void clickDuringAnimationSnapsFirst() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = arrive(c, 2, 0);
        c.update(t + 50);
        c.next(t + 100);
        assertEquals(2, c.index());
        assertTrue(c.wipe().isEmpty());
        List<Cue> cues = c.update(t + 101);
        assertEquals(1, cues.size());
        assertTrue(cues.get(0).landing());
        assertTrue(c.update(t + 5000).isEmpty());
        c.next(t + 5001);
        assertTrue(c.wipe().isPresent());
    }

    @Test
    void wipeSwapsAtHalfway() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = 5000;
        c.update(t);
        c.next(t);
        assertTrue(c.wipe().isPresent());
        c.update(t + 259);
        assertEquals(0, c.index());
        c.update(t + 260);
        assertEquals(1, c.index());
    }

    @Test
    void clickDuringWipeFinishesWithoutSkipping() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = 5000;
        c.update(t);
        c.next(t);
        c.next(t + 100);
        assertEquals(1, c.index());
        assertTrue(c.wipe().isEmpty());
    }

    @Test
    void rapidClicksNeverSkipACard() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        int lastIndex = c.index();
        int lastStep = c.step();
        List<Cue> visit = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long now = i * 10L;
            c.next(now);
            List<Cue> cues = c.update(now);
            assertTrue(c.index() - lastIndex <= 1);
            if (c.index() != lastIndex || c.step() != lastStep) {
                visit.clear();
            }
            for (Cue cue : cues) {
                if (cue.landing()) {
                    assertFalse(visit.contains(cue), "landing cue repeated: " + cue);
                    visit.add(cue);
                }
            }
            lastIndex = c.index();
            lastStep = c.step();
        }
        assertTrue(c.index() <= 4);
    }

    @Test
    void landingCueFiresOncePerVisitWithFastClicks() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        Set<String> seen = new HashSet<>();
        int lastIndex = -1;
        int lastStep = -1;
        for (int i = 0; i < 60; i++) {
            long now = i * 37L;
            if (i % 3 == 0) {
                c.next(now);
            }
            List<Cue> cues = c.update(now);
            if (c.index() != lastIndex || c.step() != lastStep) {
                seen.clear();
                lastIndex = c.index();
                lastStep = c.step();
            }
            for (Cue cue : cues) {
                if (cue.landing()) {
                    assertTrue(seen.add(cue.toString()), "landing cue repeated: " + cue);
                }
            }
        }
    }

    @Test
    void guessAutoAdvances() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t0 = arrive(c, 1, 0);
        long t = t0 + 1000;
        c.pickGuess(1200, t);
        List<Cue> cues = c.update(t);
        assertTrue(cues.contains(new Cue(t, Sound.CLICK, Effect.NONE, Anchor.CENTER, 0, 0, false)));
        assertEquals(Optional.of(1200), c.guessPick());
        assertTrue(c.update(t + 1).isEmpty());
        c.pickGuess(400, t + 2);
        assertEquals(Optional.of(1200), c.guessPick());
        c.update(t + StageController.GUESS_DELAY_MS + 260);
        assertEquals(2, c.index());
        assertEquals(HOURS, c.card().orElseThrow());
        assertEquals(Optional.of(1200), c.guessPick());
        long b = t + StageController.GUESS_DELAY_MS + 3000;
        c.update(b);
        c.back(b);
        c.update(b + 300);
        assertEquals(1, c.index());
        assertTrue(c.guessPick().isEmpty());
    }

    @Test
    void nextOnGuessWithoutPickSkips() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t0 = arrive(c, 1, 0);
        long t = t0 + 1000;
        c.next(t);
        assertTrue(c.wipe().isPresent());
        c.update(t + 260);
        assertEquals(2, c.index());
        assertTrue(c.guessPick().isEmpty());
    }

    @Test
    void countdownStepsMatchRanked() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = arrive(c, 3, 0);
        assertEquals(0, c.step());
        t += 1400;
        c.update(t);
        c.next(t);
        assertEquals(1, c.step());
        assertTrue(c.wipe().isEmpty());
        assertTrue(c.animating(t + 1));
        t += 1400;
        c.update(t);
        c.next(t);
        assertEquals(2, c.step());
        assertTrue(c.wipe().isEmpty());
        t += 1400;
        c.update(t);
        c.next(t);
        assertTrue(c.wipe().isPresent());
        assertEquals(3, c.index());

        StageController one = new StageController(deck(COUNTDOWN1), 0);
        long u = arrive(one, 3, 0) + 1400;
        one.update(u);
        one.next(u);
        assertTrue(one.wipe().isPresent());
        assertEquals(0, one.step());
    }

    @Test
    void countdownRowCuesFollowRowClock() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = arrive(c, 3, 0);
        assertTrue(c.update(t + 1).isEmpty());
        t += 1400;
        c.next(t);
        assertEquals(0, c.stepElapsed(t));
        List<Cue> second = c.update(t);
        assertEquals(1, second.size());
        assertEquals(1, second.get(0).index());
    }

    @Test
    void backShowsCardAtRest() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = arrive(c, 2, 0) + 4000;
        c.update(t);
        c.back(t);
        assertTrue(c.wipe().isPresent());
        assertTrue(c.update(t + 260).isEmpty());
        assertEquals(1, c.index());
        c.update(t + 520);
        assertFalse(c.animating(t + 520));
        assertTrue(c.update(t + 600).isEmpty());

        StageController d = new StageController(deck(COUNTDOWN3), 0);
        long u = arrive(d, 3, 0) + 2000;
        d.update(u);
        d.back(u);
        assertTrue(d.update(u + 260).isEmpty());
        assertEquals(2, d.index());
        d.update(u + 520);
        assertFalse(d.animating(u + 520));
        assertEquals(Long.MAX_VALUE / 4, d.elapsed(u + 520));
        assertTrue(d.update(u + 10000).isEmpty());
        assertTrue(d.tick(u + 900).isEmpty());

        StageController e = new StageController(deck(COUNTDOWN3), 0);
        long v = arrive(e, 4, 0) + 2000;
        e.update(v);
        e.back(v);
        e.update(v + 520);
        assertEquals(3, e.index());
        assertEquals(2, e.step());
        assertFalse(e.animating(v + 520));
        e.back(v + 600);
        assertEquals(1, e.step());
        assertTrue(e.wipe().isEmpty());
        assertFalse(e.animating(v + 600));
        assertTrue(e.update(v + 700).isEmpty());
    }

    @Test
    void skipWipesToSummaryWithCues() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        c.skip(100);
        assertTrue(c.wipe().isPresent());
        c.update(360);
        assertEquals(4, c.index());
        List<Cue> cues = c.update(360 + 200);
        assertEquals(1, cues.size());
        assertEquals(Sound.FANFARE, cues.get(0).sound());
    }

    @Test
    void deckEdgesAreInert() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        c.update(5000);
        c.back(5000);
        assertTrue(c.wipe().isEmpty());
        assertEquals(0, c.index());

        long t = arrive(c, 4, 6000) + 2000;
        c.update(t);
        c.next(t);
        assertTrue(c.wipe().isEmpty());
        assertEquals(4, c.index());
        c.skip(t + 10);
        assertTrue(c.wipe().isEmpty());
        assertEquals(4, c.index());

        StageController empty = new StageController(List.of(), 0);
        empty.next(10);
        empty.back(20);
        empty.skip(30);
        empty.pickGuess(1, 40);
        assertTrue(empty.update(50).isEmpty());
        assertTrue(empty.card().isEmpty());
        assertTrue(empty.wipe().isEmpty());
        assertTrue(empty.guessPick().isEmpty());
        assertFalse(empty.animating(60));
        assertTrue(empty.tick(70).isEmpty());
        assertEquals(0, empty.size());
        assertEquals(0, empty.index());
        assertEquals(0, empty.step());
        empty.elapsed(80);
        empty.stepElapsed(90);
    }

    @Test
    void ticksDuringCountOnly() {
        StageController c = new StageController(deck(COUNTDOWN3), 0);
        long t = arrive(c, 2, 0);
        assertTrue(c.tick(t + 399).isEmpty());
        OptionalDouble first = c.tick(t + 400);
        assertTrue(first.isPresent());
        assertEquals(0.5, first.getAsDouble(), 1e-9);
        assertTrue(c.tick(t + 430).isEmpty());
        OptionalDouble second = c.tick(t + 460);
        assertTrue(second.isPresent());
        assertEquals(0.5 + 1.5 * 60 / 2200.0, second.getAsDouble(), 1e-9);
        assertTrue(c.tick(t + 2600).isEmpty());
        assertTrue(c.tick(t + 3000).isEmpty());
    }
}
