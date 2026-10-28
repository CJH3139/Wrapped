package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class YearPickerTest {
    private static SortedSet<Integer> years(Integer... ys) {
        return new TreeSet<>(Set.of(ys));
    }

    @Test
    void initialPrefersCurrentYear() {
        assertEquals(2026, YearPicker.initial(years(2024, 2026), 2026));
    }

    @Test
    void initialFallsBackToLatest() {
        assertEquals(2025, YearPicker.initial(years(2023, 2025), 2026));
    }

    @Test
    void initialWithNoYearsIsCurrent() {
        assertEquals(2026, YearPicker.initial(years(), 2026));
    }

    @Test
    void previousAndNextSkipGaps() {
        SortedSet<Integer> ys = years(2020, 2023, 2026);
        assertEquals(OptionalInt.of(2023), YearPicker.previous(ys, 2026));
        assertEquals(OptionalInt.of(2026), YearPicker.next(ys, 2023));
        assertEquals(OptionalInt.empty(), YearPicker.previous(ys, 2020));
        assertEquals(OptionalInt.empty(), YearPicker.next(ys, 2026));
    }

    @Test
    void neighboursOfYearNotInSet() {
        SortedSet<Integer> ys = years(2020, 2023);
        assertEquals(OptionalInt.of(2023), YearPicker.previous(ys, 2026));
        assertEquals(OptionalInt.empty(), YearPicker.next(ys, 2026));
    }

    @Test
    void noYearsMeansNoDeckSoTheScreenSaysNothingToShow() {
        java.util.List<com.wrapped.cards.Card> title = java.util.List.of(new com.wrapped.cards.Card.Title(2026));
        WrappedData none = new WrappedData(years(), y -> title, y -> title);
        assertEquals(java.util.List.of(), YearPicker.deck(none, 2026, false));
        assertEquals(java.util.List.of(), YearPicker.deck(none, 2026, true));
        WrappedData some = new WrappedData(years(2026), y -> title, y -> java.util.List.of());
        assertEquals(title, YearPicker.deck(some, 2026, false));
        assertEquals(java.util.List.of(), YearPicker.deck(some, 2026, true));
        WrappedData nulls = new WrappedData(years(2026), y -> null, y -> null);
        assertEquals(java.util.List.of(), YearPicker.deck(nulls, 2026, false));
    }
}
