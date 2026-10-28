package com.wrapped.gui;

import java.util.Optional;

record StageView(int step, Optional<Integer> guessPick, boolean hideTopWord, boolean total, boolean hasPreviousYear,
        boolean hasNextYear, boolean exporting, boolean reduceMotion, int index, int size, long stepElapsedMs,
        int year) {
}
