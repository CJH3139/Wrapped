package com.wrapped.gui;

import com.wrapped.cards.Card;

final class StageRenderer {
    private StageRenderer() {
    }

    static void render(StageFrame f, Card card) {
        switch (card) {
            case Card.Title c -> HookCards.title(f, c);
            case Card.Guess c -> HookCards.guess(f, c);
            case Card.HoursReveal c -> HookCards.hoursReveal(f, c);
            case Card.ServerCount c -> PlaceCards.serverCount(f, c);
            case Card.Countdown c -> PlaceCards.countdown(f, c);
            case Card.TopServer c -> PlaceCards.topServer(f, c);
            case Card.LongestSession c -> PlaceCards.longestSession(f, c);
            case Card.PlayTime c -> HabitCards.playTime(f, c);
            case Card.Streak c -> HabitCards.streak(f, c);
            case Card.ChatCard c -> HabitCards.chat(f, c);
            case Card.Blocks c -> ActionCards.blocks(f, c);
            case Card.Fights c -> ActionCards.fights(f, c);
            case Card.Type c -> ActionCards.type(f, c);
            case Card.Summary c -> ActionCards.summary(f, c);
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
