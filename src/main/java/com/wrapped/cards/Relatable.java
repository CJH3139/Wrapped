package com.wrapped.cards;

import java.time.Duration;

public final class Relatable {
    private static final long DAY = 24 * 3600;
    private static final double ALMOST = 0.75;
    private static final long MOVIE = 2 * 3600;

    private Relatable() {
    }

    public static String hours(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 3600) {
            return "that's less than an hour";
        }
        if (seconds < MOVIE * 2) {
            return "that's about one movie";
        }
        if (seconds < DAY) {
            return "that's " + seconds / MOVIE + " movies back to back";
        }
        long days = seconds / DAY;
        double fraction = (seconds - days * DAY) / (double) DAY;
        if (fraction >= ALMOST) {
            return "that's almost " + (days + 1) + " days";
        }
        return days == 1 ? "that's a full day" : "that's " + days + " full days";
    }
}
