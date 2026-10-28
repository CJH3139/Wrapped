package com.wrapped.gui;

import java.time.Duration;
import java.util.Locale;

public final class Numbers {
    private static final String[] UNITS = {"", "k", "M", "B", "T"};

    private Numbers() {
    }

    public static String compact(long value) {
        if (value < 0) {
            return "-" + compact(-value);
        }
        if (value < 1000) {
            return Long.toString(value);
        }
        double scaled = value;
        int unit = 0;
        while (unit < UNITS.length - 1 && Math.round(scaled * 10) / 10.0 >= 1000) {
            scaled /= 1000;
            unit++;
        }
        double rounded = Math.round(scaled * 10) / 10.0;
        String text = rounded == Math.rint(rounded) ? Long.toString((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
        return text + UNITS[unit];
    }

    public static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static String duration(Duration d) {
        long minutes = Math.max(0, d.toMinutes());
        long h = minutes / 60;
        long m = minutes % 60;
        if (h == 0) {
            return m + "m";
        }
        if (h >= 100 || m == 0) {
            return h + "h";
        }
        return h + "h " + m + "m";
    }

    public static String hours(Duration d) {
        long h = Math.max(0, d.toHours());
        return h < 1 ? "<1h" : grouped(h) + "h";
    }
}
