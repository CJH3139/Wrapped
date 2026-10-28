package com.wrapped.gui;

import com.wrapped.gui.CardTiming.Cue;
import java.util.ArrayList;
import java.util.List;

final class CueTracker {
    private final List<Cue> cues;
    private final boolean[] fired;

    CueTracker(List<Cue> cues) {
        this.cues = List.copyOf(cues);
        this.fired = new boolean[this.cues.size()];
    }

    List<Cue> due(long elapsedMs) {
        List<Cue> out = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            if (!fired[i] && cues.get(i).atMs() <= elapsedMs) {
                fired[i] = true;
                out.add(cues.get(i));
            }
        }
        return out;
    }

    List<Cue> snap() {
        List<Cue> out = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            if (!fired[i]) {
                fired[i] = true;
                if (cues.get(i).landing()) {
                    out.add(cues.get(i));
                }
            }
        }
        return out;
    }
}
