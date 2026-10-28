package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.gui.CardTiming.Cue;
import java.util.List;
import org.junit.jupiter.api.Test;

class CueTrackerTest {
    private static Cue cue(long at, boolean landing) {
        return new Cue(at, Sound.CLICK, Effect.NONE, Anchor.CENTER, 0, 0, landing);
    }

    @Test
    void dueReturnsEachCueOnce() {
        CueTracker t = new CueTracker(List.of(cue(100, false), cue(200, false)));
        assertEquals(List.of(cue(100, false)), t.due(150));
        assertTrue(t.due(150).isEmpty());
        assertEquals(List.of(cue(200, false)), t.due(500));
    }

    @Test
    void snapReturnsOnlyUnfiredLandings() {
        CueTracker t = new CueTracker(List.of(cue(100, false), cue(300, true)));
        t.due(150);
        assertEquals(List.of(cue(300, true)), t.snap());
        assertTrue(t.due(1000).isEmpty());
    }
}
