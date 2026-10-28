package com.wrapped.gui;

import com.wrapped.cards.Card;

final class StageRenderer {
    private StageRenderer() {
    }

    static void render(StageFrame f, Card card) {
        if (card instanceof Card.Title c) {
            HookCards.title(f, c);
        } else if (card instanceof Card.Guess c) {
            HookCards.guess(f, c);
        } else if (card instanceof Card.HoursReveal c) {
            HookCards.hoursReveal(f, c);
        } else if (card instanceof Card.ServerCount c) {
            PlaceCards.serverCount(f, c);
        } else if (card instanceof Card.Countdown c) {
            PlaceCards.countdown(f, c);
        } else if (card instanceof Card.TopServer c) {
            PlaceCards.topServer(f, c);
        } else if (card instanceof Card.LongestSession c) {
            PlaceCards.longestSession(f, c);
        } else if (card instanceof Card.PlayTime c) {
            HabitCards.playTime(f, c);
        } else if (card instanceof Card.Streak c) {
            HabitCards.streak(f, c);
        } else if (card instanceof Card.ChatCard c) {
            HabitCards.chat(f, c);
        } else if (card instanceof Card.Blocks c) {
            ActionCards.blocks(f, c);
        } else if (card instanceof Card.Fights c) {
            ActionCards.fights(f, c);
        } else if (card instanceof Card.Type c) {
            ActionCards.type(f, c);
        } else if (card instanceof Card.Summary c) {
            ActionCards.summary(f, c);
        }
    }

    static float[] anchor(StageFrame f, Card card, Anchor a, int index) {
        return HookCards.anchor(f, card, a, index)
                .or(() -> PlaceCards.anchor(f, card, a, index))
                .or(() -> HabitCards.anchor(f, card, a, index))
                .or(() -> ActionCards.anchor(f, card, a, index))
                .orElseGet(() -> new float[] {f.w() / 2f, f.h() / 2f});
    }
}
