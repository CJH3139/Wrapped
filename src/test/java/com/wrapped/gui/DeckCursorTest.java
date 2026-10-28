package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.cards.Card;
import com.wrapped.cards.PlayerType;
import com.wrapped.stats.ServerStat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeckCursorTest {
    private static ServerStat server(String host) {
        return new ServerStat(host, Duration.ofHours(3), 1, 1, LocalDate.of(2026, 1, 1));
    }

    private static final Card TITLE = new Card.Title(2026);
    private static final Card COUNTDOWN = new Card.Countdown(List.of(server("e"), server("d"), server("c"),
            server("b")));
    private static final Card TOP = new Card.TopServer(server("a"));
    private static final Card SUMMARY = new Card.Summary(10, Optional.of("a"), 3, PlayerType.EXPLORER);

    @Test
    void emptyDeckHasNoCurrent() {
        DeckCursor c = new DeckCursor(List.of());
        assertTrue(c.current().isEmpty());
        assertFalse(c.next());
        assertFalse(c.back());
        c.skip();
        assertTrue(c.current().isEmpty());
    }

    @Test
    void countdownStepsOneServerPerAdvance() {
        DeckCursor c = new DeckCursor(List.of(TITLE, COUNTDOWN, TOP));
        c.next();
        assertEquals(COUNTDOWN, c.current().orElseThrow());
        assertEquals(0, c.step());
        for (int step = 1; step <= 3; step++) {
            assertTrue(c.next());
            assertEquals(1, c.index());
            assertEquals(step, c.step());
        }
        assertTrue(c.next());
        assertEquals(TOP, c.current().orElseThrow());
        assertEquals(0, c.step());
    }

    @Test
    void backStepsThroughCountdown() {
        DeckCursor c = new DeckCursor(List.of(TITLE, COUNTDOWN, TOP));
        for (int i = 0; i < 5; i++) {
            c.next();
        }
        assertEquals(TOP, c.current().orElseThrow());
        c.back();
        assertEquals(COUNTDOWN, c.current().orElseThrow());
        assertEquals(3, c.step());
        c.back();
        assertEquals(2, c.step());
        c.back();
        c.back();
        assertEquals(0, c.step());
        assertEquals(1, c.index());
        c.back();
        assertEquals(TITLE, c.current().orElseThrow());
        assertFalse(c.back());
    }

    @Test
    void nextStopsAtLastCard() {
        DeckCursor c = new DeckCursor(List.of(TITLE, SUMMARY));
        assertTrue(c.next());
        assertFalse(c.next());
        assertEquals(1, c.index());
    }

    @Test
    void skipGoesToSummary() {
        DeckCursor c = new DeckCursor(List.of(TITLE, COUNTDOWN, SUMMARY, TOP));
        c.skip();
        assertEquals(SUMMARY, c.current().orElseThrow());
    }

    @Test
    void skipFallsBackToLastCard() {
        DeckCursor c = new DeckCursor(List.of(TITLE, COUNTDOWN, TOP));
        c.skip();
        assertEquals(TOP, c.current().orElseThrow());
        assertEquals(0, c.step());
    }

    @Test
    void stepsForCountdown() {
        assertEquals(4, DeckCursor.steps(COUNTDOWN));
        assertEquals(1, DeckCursor.steps(TOP));
        assertEquals(1, DeckCursor.steps(new Card.Countdown(List.of())));
    }
}
