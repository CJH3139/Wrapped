package com.wrapped.gui;

import com.wrapped.cards.Card;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

final class CardTiming {
    private CardTiming() {
    }

    record Cue(long atMs, Sound sound, Effect effect, Anchor anchor, int index, int shakeMs, boolean landing) {
    }

    record CountWindow(long startMs, long durationMs, long target) {
    }

    static List<Cue> cues(Card card, int step) {
        List<Cue> out = new ArrayList<>();
        switch (card) {
            case Card.Title t -> {
                int digits = Integer.toString(t.year()).length();
                for (int i = 0; i < digits; i++) {
                    boolean last = i == digits - 1;
                    out.add(new Cue(250 + i * 220L + 330, Sound.THUD, Effect.PUFF, Anchor.DIGIT, i,
                            last ? 120 : 0, last));
                }
            }
            case Card.Guess g -> {
            }
            case Card.HoursReveal h ->
                out.add(new Cue(2600, Sound.CHIME, Effect.BIG_BURST, Anchor.HERO, 0, 320, true));
            case Card.ServerCount s -> out.add(heroLight());
            case Card.Streak s -> out.add(heroLight());
            case Card.ChatCard c -> {
                out.add(heroLight());
                if (c.topWord().isPresent()) {
                    out.add(new Cue(2300, Sound.THUD, Effect.PUFF, Anchor.STAMP, 0, 150, true));
                }
            }
            case Card.Blocks b -> out.add(heroLight());
            case Card.LongestSession l -> out.add(heroLight());
            case Card.Countdown c -> out.add(new Cue(0, Sound.LIGHT, Effect.NONE, Anchor.ROW, step, 0, true));
            case Card.TopServer t -> {
                out.add(new Cue(250, Sound.FANFARE, Effect.BIG_BURST, Anchor.CENTER, 0, 360, true));
                out.add(new Cue(900, Sound.FIREWORK, Effect.BURST, Anchor.LEFT, 0, 0, false));
                out.add(new Cue(1300, Sound.FIREWORK, Effect.BURST, Anchor.RIGHT, 0, 0, false));
            }
            case Card.PlayTime p -> out.add(new Cue(1600, Sound.THUD, Effect.BURST, Anchor.STAMP, 0, 220, true));
            case Card.Fights f -> {
                int n = fightStats(f);
                for (int k = 0; k < n; k++) {
                    out.add(new Cue(300 + k * 300L, Sound.HIT, Effect.PUFF, Anchor.ROW, k, 0, k == n - 1));
                }
            }
            case Card.Type t -> out.add(new Cue(500, Sound.CHIME, Effect.BIG_BURST, Anchor.STAMP, 0, 300, true));
            case Card.Summary s -> out.add(new Cue(200, Sound.FANFARE, Effect.CONFETTI, Anchor.CENTER, 0, 0, true));
        }
        out.sort(Comparator.comparingLong(Cue::atMs));
        return out;
    }

    static long duration(Card card, int step) {
        return switch (card) {
            case Card.Title t -> 250 + (Integer.toString(t.year()).length() - 1) * 220L + 330 + 200;
            case Card.Guess g -> 0;
            case Card.HoursReveal h -> 3000;
            case Card.ServerCount s -> 2300;
            case Card.Streak s -> 2350;
            case Card.ChatCard c -> c.topWord().isPresent() ? 2750 : 2300;
            case Card.Blocks b -> 2300;
            case Card.LongestSession l -> 2300;
            case Card.Countdown c -> 1350;
            case Card.TopServer t -> 1500;
            case Card.PlayTime p -> 2050;
            case Card.Fights f -> 300 + fightStats(f) * 300L + 300;
            case Card.Type t -> 950;
            case Card.Summary s -> 600;
        };
    }

    static Optional<CountWindow> count(Card card) {
        return switch (card) {
            case Card.HoursReveal h -> Optional.of(new CountWindow(400, 2200, h.hours()));
            case Card.ServerCount s -> Optional.of(new CountWindow(400, 1500, s.count()));
            case Card.Streak s -> Optional.of(new CountWindow(400, 1500, s.days()));
            case Card.ChatCard c -> Optional.of(new CountWindow(400, 1500, c.messagesSent()));
            case Card.Blocks b -> Optional.of(new CountWindow(400, 1500, Math.max(b.mined(), b.placed())));
            case Card.LongestSession l -> Optional.of(new CountWindow(400, 1500, l.duration().toMinutes()));
            default -> Optional.empty();
        };
    }

    static long countValue(CountWindow w, long elapsedMs) {
        if (elapsedMs < w.startMs()) {
            return 0;
        }
        return CountUp.valueAt(w.target(), elapsedMs - w.startMs(), w.durationMs());
    }

    private static Cue heroLight() {
        return new Cue(1900, Sound.LIGHT, Effect.BURST, Anchor.HERO, 0, 200, true);
    }

    private static int fightStats(Card.Fights f) {
        int n = 1;
        for (OptionalLong v : new OptionalLong[] {f.heartsDealt(), f.heartsTaken(), f.mobKills(), f.playersKilled()}) {
            if (v.isPresent()) {
                n++;
            }
        }
        return n;
    }
}
