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
        if (card instanceof Card.Title t) {
            int digits = Integer.toString(t.year()).length();
            for (int i = 0; i < digits; i++) {
                boolean last = i == digits - 1;
                out.add(new Cue(250 + i * 220L + 330, Sound.THUD, Effect.PUFF, Anchor.DIGIT, i,
                        last ? 120 : 0, last));
            }
        } else if (card instanceof Card.Guess g) {
        } else if (card instanceof Card.HoursReveal h) {
            out.add(new Cue(2600, Sound.CHIME, Effect.BIG_BURST, Anchor.HERO, 0, 320, true));
        } else if (card instanceof Card.ServerCount s) {
            out.add(heroLight());
        } else if (card instanceof Card.Streak s) {
            out.add(heroLight());
        } else if (card instanceof Card.ChatCard c) {
            out.add(heroLight());
            if (c.topWord().isPresent()) {
                out.add(new Cue(2300, Sound.THUD, Effect.PUFF, Anchor.STAMP, 0, 150, true));
            }
        } else if (card instanceof Card.Blocks b) {
            out.add(heroLight());
        } else if (card instanceof Card.LongestSession l) {
            out.add(heroLight());
        } else if (card instanceof Card.Countdown c) {
            out.add(new Cue(0, Sound.LIGHT, Effect.NONE, Anchor.ROW, step, 0, true));
        } else if (card instanceof Card.TopServer t) {
            out.add(new Cue(250, Sound.FANFARE, Effect.BIG_BURST, Anchor.CENTER, 0, 360, true));
            out.add(new Cue(900, Sound.FIREWORK, Effect.BURST, Anchor.LEFT, 0, 0, false));
            out.add(new Cue(1300, Sound.FIREWORK, Effect.BURST, Anchor.RIGHT, 0, 0, false));
        } else if (card instanceof Card.PlayTime p) {
            out.add(new Cue(1600, Sound.THUD, Effect.BURST, Anchor.STAMP, 0, 220, true));
        } else if (card instanceof Card.Fights f) {
            int n = fightStats(f);
            for (int k = 0; k < n; k++) {
                out.add(new Cue(300 + k * 300L, Sound.HIT, Effect.PUFF, Anchor.ROW, k, 0, k == n - 1));
            }
        } else if (card instanceof Card.Type t) {
            out.add(new Cue(500, Sound.CHIME, Effect.BIG_BURST, Anchor.STAMP, 0, 300, true));
        } else if (card instanceof Card.Summary s) {
            out.add(new Cue(200, Sound.FANFARE, Effect.CONFETTI, Anchor.CENTER, 0, 0, true));
        }
        out.sort(Comparator.comparingLong(Cue::atMs));
        return out;
    }

    static long duration(Card card, int step) {
        if (card instanceof Card.Title t) {
            return 250 + (Integer.toString(t.year()).length() - 1) * 220L + 330 + 200;
        } else if (card instanceof Card.Guess g) {
            return 0;
        } else if (card instanceof Card.HoursReveal h) {
            return 3000;
        } else if (card instanceof Card.ServerCount s) {
            return 2300;
        } else if (card instanceof Card.Streak s) {
            return 2350;
        } else if (card instanceof Card.ChatCard c) {
            return c.topWord().isPresent() ? 2750 : 2300;
        } else if (card instanceof Card.Blocks b) {
            return 2300;
        } else if (card instanceof Card.LongestSession l) {
            return 2300;
        } else if (card instanceof Card.Countdown c) {
            return 1350;
        } else if (card instanceof Card.TopServer t) {
            return 1500;
        } else if (card instanceof Card.PlayTime p) {
            return 2050;
        } else if (card instanceof Card.Fights f) {
            return 300 + fightStats(f) * 300L + 300;
        } else if (card instanceof Card.Type t) {
            return 950;
        } else if (card instanceof Card.Summary s) {
            return 600;
        }
        throw new IllegalStateException("Unknown " + card);
    }

    static Optional<CountWindow> count(Card card) {
        if (card instanceof Card.HoursReveal h) {
            return Optional.of(new CountWindow(400, 2200, h.hours()));
        } else if (card instanceof Card.ServerCount s) {
            return Optional.of(new CountWindow(400, 1500, s.count()));
        } else if (card instanceof Card.Streak s) {
            return Optional.of(new CountWindow(400, 1500, s.days()));
        } else if (card instanceof Card.ChatCard c) {
            return Optional.of(new CountWindow(400, 1500, c.messagesSent()));
        } else if (card instanceof Card.Blocks b) {
            return Optional.of(new CountWindow(400, 1500, Math.max(b.mined(), b.placed())));
        } else if (card instanceof Card.LongestSession l) {
            return Optional.of(new CountWindow(400, 1500, l.duration().toMinutes()));
        }
        return Optional.empty();
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
