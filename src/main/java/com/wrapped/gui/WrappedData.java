package com.wrapped.gui;

import com.wrapped.cards.Card;
import java.util.List;
import java.util.SortedSet;
import java.util.function.IntFunction;

public record WrappedData(SortedSet<Integer> years, IntFunction<List<Card>> instanceDeck,
        IntFunction<List<Card>> totalDeck) {
}
