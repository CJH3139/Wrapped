package com.wrapped.gui;

import com.wrapped.cards.Card;
import java.util.List;
import java.util.Optional;

final class DeckCursor {
    private final List<Card> deck;
    private int index;
    private int step;

    DeckCursor(List<Card> deck) {
        this.deck = List.copyOf(deck);
    }

    static int steps(Card card) {
        return card instanceof Card.Countdown c ? Math.max(1, c.ranked().size()) : 1;
    }

    Optional<Card> current() {
        return index < deck.size() ? Optional.of(deck.get(index)) : Optional.empty();
    }

    int index() {
        return index;
    }

    int step() {
        return step;
    }

    int size() {
        return deck.size();
    }

    boolean next() {
        Optional<Card> card = current();
        if (card.isEmpty()) {
            return false;
        }
        if (step < steps(card.get()) - 1) {
            step++;
            return true;
        }
        if (index < deck.size() - 1) {
            index++;
            step = 0;
            return true;
        }
        return false;
    }

    boolean back() {
        if (current().isEmpty()) {
            return false;
        }
        if (step > 0) {
            step--;
            return true;
        }
        if (index > 0) {
            index--;
            step = steps(deck.get(index)) - 1;
            return true;
        }
        return false;
    }

    void skip() {
        if (deck.isEmpty()) {
            return;
        }
        int target = deck.size() - 1;
        for (int i = deck.size() - 1; i >= 0; i--) {
            if (deck.get(i) instanceof Card.Summary) {
                target = i;
                break;
            }
        }
        index = target;
        step = 0;
    }
}
