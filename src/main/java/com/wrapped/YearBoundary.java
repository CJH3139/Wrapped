package com.wrapped;

import java.time.LocalDateTime;
import java.util.OptionalInt;

final class YearBoundary {
    private int year;

    YearBoundary(int year) {
        this.year = year;
    }

    OptionalInt crossed(int now) {
        if (now == year) {
            return OptionalInt.empty();
        }
        int old = year;
        year = now;
        return OptionalInt.of(old);
    }

    static LocalDateTime stamp(int oldYear) {
        return LocalDateTime.of(oldYear, 12, 31, 23, 59, 59);
    }
}
