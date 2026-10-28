package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.cards.Card;
import com.wrapped.gui.CardTiming.CountWindow;
import com.wrapped.gui.CardTiming.Cue;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CardTimingTest {
    @Test
    void hoursRevealLandsAfterCount() {
        Card c = new Card.HoursReveal(1218, "x", Optional.empty(), 2025);
        assertEquals(new CountWindow(400, 2200, 1218), CardTiming.count(c).orElseThrow());
        List<Cue> cues = CardTiming.cues(c, 0);
        assertEquals(1, cues.size());
        assertEquals(2600, cues.get(0).atMs());
        assertEquals(Sound.CHIME, cues.get(0).sound());
        assertEquals(Effect.BIG_BURST, cues.get(0).effect());
        assertTrue(cues.get(0).landing());
        assertEquals(3000, CardTiming.duration(c, 0));
    }

    @Test
    void titleHasOneCuePerDigit() {
        Card c = new Card.Title(2026);
        List<Cue> cues = CardTiming.cues(c, 0);
        assertEquals(4, cues.size());
        long[] at = {580, 800, 1020, 1240};
        for (int i = 0; i < 4; i++) {
            assertEquals(at[i], cues.get(i).atMs());
            assertEquals(i, cues.get(i).index());
            assertEquals(i == 3, cues.get(i).landing());
            assertEquals(i == 3 ? 120 : 0, cues.get(i).shakeMs());
        }
        assertEquals(1440, CardTiming.duration(c, 0));
    }

    @Test
    void fightsSkipsAbsentStats() {
        Card c = new Card.Fights(3, OptionalLong.of(10), OptionalLong.empty(), OptionalLong.empty(),
                OptionalLong.empty(), Optional.empty());
        List<Cue> cues = CardTiming.cues(c, 0);
        assertEquals(2, cues.size());
        assertEquals(300, cues.get(0).atMs());
        assertEquals(600, cues.get(1).atMs());
        assertFalse(cues.get(0).landing());
        assertTrue(cues.get(1).landing());
        assertEquals(1200, CardTiming.duration(c, 0));
    }

    @Test
    void countValueStartsAtZero() {
        assertEquals(0, CardTiming.countValue(new CountWindow(400, 1500, 100), 399));
        assertEquals(100, CardTiming.countValue(new CountWindow(400, 1500, 100), 1900));
    }

    @Test
    void guessIsAlwaysAtRest() {
        Card c = new Card.Guess(10, List.of(1, 2, 3));
        assertEquals(0, CardTiming.duration(c, 0));
        assertTrue(CardTiming.cues(c, 0).isEmpty());
    }

    @Test
    void chatCardWithWordAddsStamp() {
        Card c = new Card.ChatCard(5, Optional.of("lol"));
        assertEquals(2, CardTiming.cues(c, 0).size());
        assertEquals(2750, CardTiming.duration(c, 0));
        assertEquals(2300, CardTiming.duration(new Card.ChatCard(5, Optional.empty()), 0));
    }

    @Test
    void countdownCueUsesStep() {
        Card c = new Card.Countdown(List.of());
        List<Cue> cues = CardTiming.cues(c, 2);
        assertEquals(2, cues.get(0).index());
        assertEquals(Anchor.ROW, cues.get(0).anchor());
        assertEquals(1350, CardTiming.duration(c, 2));
    }
}
