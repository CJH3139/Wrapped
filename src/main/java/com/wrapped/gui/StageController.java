package com.wrapped.gui;

import com.wrapped.cards.Card;
import com.wrapped.gui.CardTiming.Cue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

final class StageController {
    static final long GUESS_DELAY_MS = 450;
    static final long TICK_GAP_MS = 60;
    private static final long AT_REST = Long.MAX_VALUE / 4;

    private enum Move {
        FORWARD, BACK, SKIP
    }

    private final DeckCursor cursor;
    private final int summaryIndex;
    private final List<Cue> pending = new ArrayList<>();
    private CueTracker tracker = new CueTracker(List.of());
    private long cardStart;
    private long stepStart;
    private boolean snapped;
    private BlockWipe wipe;
    private Move move;
    private Optional<Integer> guessPick = Optional.empty();
    private OptionalLong advanceAt = OptionalLong.empty();
    private OptionalLong lastTick = OptionalLong.empty();

    StageController(List<Card> deck, long nowMs) {
        this.cursor = new DeckCursor(deck);
        DeckCursor probe = new DeckCursor(deck);
        probe.skip();
        this.summaryIndex = probe.index();
        show(nowMs, false);
    }

    void next(long nowMs) {
        if (cursor.current().isEmpty()) {
            return;
        }
        settle(nowMs);
        advance(nowMs);
    }

    void back(long nowMs) {
        if (cursor.current().isEmpty()) {
            return;
        }
        settle(nowMs);
        if (wipe != null) {
            finishWipe(nowMs);
            return;
        }
        if (cursor.step() > 0) {
            cursor.back();
            stepStart = nowMs;
            snapped = true;
            tracker = new CueTracker(List.of());
            return;
        }
        if (cursor.index() > 0) {
            startWipe(nowMs, Move.BACK);
        }
    }

    void skip(long nowMs) {
        if (cursor.current().isEmpty()) {
            return;
        }
        settle(nowMs);
        if (wipe != null) {
            finishWipe(nowMs);
        }
        if (cursor.index() != summaryIndex) {
            startWipe(nowMs, Move.SKIP);
        }
    }

    void pickGuess(int option, long nowMs) {
        Optional<Card> card = cursor.current();
        if (card.isEmpty() || !(card.get() instanceof Card.Guess) || guessPick.isPresent() || wipe != null) {
            return;
        }
        guessPick = Optional.of(option);
        pending.add(new Cue(nowMs, Sound.CLICK, Effect.NONE, Anchor.CENTER, 0, 0, false));
        advanceAt = OptionalLong.of(nowMs + GUESS_DELAY_MS);
    }

    List<Cue> update(long nowMs) {
        if (cursor.current().isEmpty()) {
            return List.of();
        }
        settle(nowMs);
        List<Cue> out = new ArrayList<>(pending);
        pending.clear();
        if (!snapped) {
            out.addAll(tracker.due(clock(nowMs)));
        }
        return out;
    }

    Optional<Card> card() {
        return cursor.current();
    }

    int index() {
        return cursor.index();
    }

    int step() {
        return cursor.step();
    }

    int size() {
        return cursor.size();
    }

    Optional<Integer> guessPick() {
        return guessPick;
    }

    Optional<BlockWipe> wipe() {
        return Optional.ofNullable(wipe);
    }

    long elapsed(long nowMs) {
        return snapped ? AT_REST : nowMs - cardStart;
    }

    long stepElapsed(long nowMs) {
        return snapped ? AT_REST : nowMs - stepStart;
    }

    boolean animating(long nowMs) {
        return wipe != null || cardAnimating(nowMs);
    }

    OptionalDouble tick(long nowMs) {
        Optional<Card> card = cursor.current();
        if (card.isEmpty() || snapped) {
            return OptionalDouble.empty();
        }
        Optional<CardTiming.CountWindow> window = CardTiming.count(card.get());
        if (window.isEmpty()) {
            return OptionalDouble.empty();
        }
        CardTiming.CountWindow w = window.get();
        long e = elapsed(nowMs);
        if (e < w.startMs() || e >= w.startMs() + w.durationMs()) {
            return OptionalDouble.empty();
        }
        if (lastTick.isPresent() && nowMs - lastTick.getAsLong() < TICK_GAP_MS) {
            return OptionalDouble.empty();
        }
        lastTick = OptionalLong.of(nowMs);
        double progress = (double) (e - w.startMs()) / w.durationMs();
        return OptionalDouble.of(0.5 + 1.5 * progress);
    }

    private void advance(long nowMs) {
        if (wipe != null) {
            finishWipe(nowMs);
            return;
        }
        if (cardAnimating(nowMs)) {
            pending.addAll(tracker.snap());
            snapped = true;
            return;
        }
        Card card = cursor.current().orElseThrow();
        if (card instanceof Card.Countdown && cursor.step() < DeckCursor.steps(card) - 1) {
            cursor.next();
            stepStart = nowMs;
            snapped = false;
            tracker = new CueTracker(CardTiming.cues(card, cursor.step()));
            return;
        }
        if (cursor.index() < cursor.size() - 1) {
            startWipe(nowMs, Move.FORWARD);
        }
    }

    private void settle(long nowMs) {
        if (advanceAt.isPresent() && nowMs >= advanceAt.getAsLong()) {
            long at = advanceAt.getAsLong();
            advanceAt = OptionalLong.empty();
            if (wipe == null) {
                advance(at);
            }
        }
        if (wipe != null) {
            if (wipe.swapDue(nowMs)) {
                swap(nowMs);
            }
            if (wipe.done(nowMs)) {
                wipe = null;
            }
        }
    }

    private void startWipe(long nowMs, Move m) {
        wipe = new BlockWipe(nowMs);
        move = m;
        advanceAt = OptionalLong.empty();
    }

    private void finishWipe(long nowMs) {
        if (!wipe.swapped()) {
            swap(nowMs);
        }
        wipe.finish();
        wipe = null;
    }

    private void swap(long nowMs) {
        wipe.markSwapped();
        switch (move) {
            case FORWARD -> {
                cursor.next();
                show(nowMs, false);
            }
            case BACK -> {
                cursor.back();
                show(nowMs, true);
            }
            case SKIP -> {
                cursor.skip();
                show(nowMs, false);
            }
        }
    }

    private void show(long nowMs, boolean atRest) {
        cardStart = nowMs;
        stepStart = nowMs;
        snapped = atRest;
        advanceAt = OptionalLong.empty();
        lastTick = OptionalLong.empty();
        Optional<Card> card = cursor.current();
        if (card.isPresent() && card.get() instanceof Card.Guess) {
            guessPick = Optional.empty();
        }
        tracker = atRest || card.isEmpty() ? new CueTracker(List.of())
                : new CueTracker(CardTiming.cues(card.get(), cursor.step()));
    }

    private boolean cardAnimating(long nowMs) {
        Optional<Card> card = cursor.current();
        if (card.isEmpty() || snapped) {
            return false;
        }
        return clock(nowMs) < CardTiming.duration(card.get(), cursor.step());
    }

    private long clock(long nowMs) {
        Optional<Card> card = cursor.current();
        return card.isPresent() && card.get() instanceof Card.Countdown ? stepElapsed(nowMs) : elapsed(nowMs);
    }
}
