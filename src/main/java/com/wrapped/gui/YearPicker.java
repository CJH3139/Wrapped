package com.wrapped.gui;

import com.wrapped.cards.Card;
import java.util.List;
import java.util.OptionalInt;
import java.util.SortedSet;

public final class YearPicker {
    private YearPicker() {
    }

    public static int initial(SortedSet<Integer> years, int now) {
        return years.isEmpty() || years.contains(now) ? now : years.last();
    }

    public static List<Card> deck(WrappedData data, int year, boolean total) {
        if (data.years().isEmpty()) {
            return List.of();
        }
        List<Card> built = (total ? data.totalDeck() : data.instanceDeck()).apply(year);
        return built == null ? List.of() : built;
    }

    public static OptionalInt previous(SortedSet<Integer> years, int year) {
        SortedSet<Integer> before = years.headSet(year);
        return before.isEmpty() ? OptionalInt.empty() : OptionalInt.of(before.last());
    }

    public static OptionalInt next(SortedSet<Integer> years, int year) {
        SortedSet<Integer> after = years.tailSet(year + 1);
        return after.isEmpty() ? OptionalInt.empty() : OptionalInt.of(after.first());
    }
}
