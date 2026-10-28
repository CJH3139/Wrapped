package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class NumbersTest {
    @Test
    void compactSmallNumbersArePlain() {
        assertEquals("0", Numbers.compact(0));
        assertEquals("999", Numbers.compact(999));
    }

    @Test
    void compactThousandsAndMillions() {
        assertEquals("1k", Numbers.compact(1000));
        assertEquals("1.2k", Numbers.compact(1234));
        assertEquals("12.3k", Numbers.compact(12_345));
        assertEquals("3.4M", Numbers.compact(3_400_000));
        assertEquals("2.1B", Numbers.compact(2_100_000_000L));
    }

    @Test
    void compactPromotesWhenRoundingReachesNextUnit() {
        assertEquals("1M", Numbers.compact(999_999));
        assertEquals("1M", Numbers.compact(999_950));
        assertEquals("10k", Numbers.compact(9_999));
    }

    @Test
    void compactNegative() {
        assertEquals("-1.2k", Numbers.compact(-1234));
    }

    @Test
    void grouped() {
        assertEquals("1,234,567", Numbers.grouped(1_234_567));
        assertEquals("12", Numbers.grouped(12));
    }

    @Test
    void durations() {
        assertEquals("45m", Numbers.duration(Duration.ofMinutes(45)));
        assertEquals("5h 12m", Numbers.duration(Duration.ofMinutes(312)));
        assertEquals("3h", Numbers.duration(Duration.ofHours(3)));
        assertEquals("120h", Numbers.duration(Duration.ofHours(120).plusMinutes(5)));
    }

    @Test
    void hours() {
        assertEquals("12h", Numbers.hours(Duration.ofHours(12).plusMinutes(20)));
        assertEquals("1,500h", Numbers.hours(Duration.ofHours(1500)));
        assertEquals("<1h", Numbers.hours(Duration.ofMinutes(20)));
    }
}
