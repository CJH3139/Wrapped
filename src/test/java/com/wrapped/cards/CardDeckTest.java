package com.wrapped.cards;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.sessions.Session;
import com.wrapped.stats.YearStats;
import com.wrapped.world.WorldStats;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CardDeckTest {
    private static boolean has(List<Card> deck, Class<? extends Card> type) {
        return deck.stream().anyMatch(type::isInstance);
    }

    private static <T extends Card> T find(List<Card> deck, Class<T> type) {
        return deck.stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static YearStats withSession(YearStats f, Session s) {
        return new YearStats(2026, f.totalTime(), f.uniqueServers(), f.serversByTime(), f.mostActive(),
                Optional.of(s), f.secondsByHour(), 0, f.firstDay(), 0, Optional.empty(), 0, 0, Optional.empty(),
                false, Duration.ZERO);
    }

    private static YearStats withTimes(YearStats f, long hours, boolean hasPrev, Duration prev) {
        return new YearStats(2026, Duration.ofHours(hours), f.uniqueServers(), f.serversByTime(), f.mostActive(),
                f.longestSession(), f.secondsByHour(), 0, f.firstDay(), 0, Optional.empty(), 0, 0,
                Optional.empty(), hasPrev, prev);
    }

    @Test
    void singleplayerOnlyPlayerHasNoServerCards() {
        YearStats y = Fixtures.base(Duration.ofHours(5), List.of(), Fixtures.evenly());
        List<Card> deck = CardDeck.build(y, Optional.of(Fixtures.world(100, 50, 0, 0)), 1);
        assertFalse(has(deck, Card.ServerCount.class));
        assertFalse(has(deck, Card.Countdown.class));
        assertFalse(has(deck, Card.TopServer.class));
        assertTrue(has(deck, Card.Blocks.class));
    }

    @Test
    void orderIsFixed() {
        List<Card> deck = CardDeck.build(Fixtures.full(), Optional.of(Fixtures.world(1000, 500, 3, 40)), 7);
        List<Class<?>> expected = List.of(Card.Title.class, Card.Guess.class, Card.HoursReveal.class,
                Card.ServerCount.class, Card.Countdown.class, Card.TopServer.class, Card.LongestSession.class,
                Card.PlayTime.class, Card.Streak.class, Card.ChatCard.class, Card.Blocks.class, Card.Fights.class,
                Card.Type.class, Card.Summary.class);
        assertEquals(expected, deck.stream().<Class<?>>map(Object::getClass).toList());
    }

    @Test
    void minimalPlayerKeepsOnlyAlwaysCards() {
        YearStats y = Fixtures.base(Duration.ofMinutes(30), List.of(), Fixtures.evenly());
        List<Card> deck = CardDeck.build(y, Optional.empty(), 1);
        assertEquals(List.of(Card.Title.class, Card.HoursReveal.class, Card.Type.class, Card.Summary.class),
                deck.stream().<Class<?>>map(Object::getClass).toList());
    }

    @Test
    void countdownShowsFiveFirstAndExcludesTop() {
        Card.Countdown c = find(CardDeck.build(Fixtures.full(), Optional.empty(), 1), Card.Countdown.class);
        assertEquals(List.of("e.net", "d.net", "c.net", "b.net"),
                c.ranked().stream().map(s -> s.host()).toList());
    }

    @Test
    void countdownWithThreeServersShowsTwoAndThree() {
        YearStats y = Fixtures.base(Duration.ofHours(10),
                List.of(Fixtures.server("a", 5), Fixtures.server("b", 3), Fixtures.server("c", 2)),
                Fixtures.evenly());
        Card.Countdown c = find(CardDeck.build(y, Optional.empty(), 1), Card.Countdown.class);
        assertEquals(List.of("c", "b"), c.ranked().stream().map(s -> s.host()).toList());
    }

    @Test
    void countdownCapsAtFiveWithManyServers() {
        List<com.wrapped.stats.ServerStat> many = List.of(Fixtures.server("a", 9), Fixtures.server("b", 8),
                Fixtures.server("c", 7), Fixtures.server("d", 6), Fixtures.server("e", 5), Fixtures.server("f", 4),
                Fixtures.server("g", 3));
        Card.Countdown c = find(CardDeck.build(Fixtures.base(Duration.ofHours(10), many, Fixtures.evenly()),
                Optional.empty(), 1), Card.Countdown.class);
        assertEquals(List.of("e", "d", "c", "b"), c.ranked().stream().map(s -> s.host()).toList());
    }

    @Test
    void hoursRevealUp() {
        Card.HoursReveal up = find(CardDeck.build(Fixtures.full(), Optional.empty(), 1), Card.HoursReveal.class);
        assertEquals(Optional.of(43), up.changeVsPreviousYearPercent());
        assertEquals(214, up.hours());
        assertEquals("that's almost 9 days", up.relatable());
        assertEquals("up 43% from 2025", up.changeText().orElseThrow());
    }

    @Test
    void hoursRevealDownAndAbsent() {
        YearStats f = Fixtures.full();
        Card.HoursReveal d = find(
                CardDeck.build(withTimes(f, 88, true, Duration.ofHours(100)), Optional.empty(), 1),
                Card.HoursReveal.class);
        assertEquals(Optional.of(-12), d.changeVsPreviousYearPercent());
        assertEquals("down 12% from 2025", d.changeText().orElseThrow());
        Card.HoursReveal t = find(
                CardDeck.build(withTimes(f, 88, true, Duration.ofMinutes(30)), Optional.empty(), 1),
                Card.HoursReveal.class);
        assertEquals(Optional.empty(), t.changeVsPreviousYearPercent());
        assertEquals(Optional.empty(), t.changeText());
        Card.HoursReveal n = find(
                CardDeck.build(withTimes(f, 88, false, Duration.ofHours(100)), Optional.empty(), 1),
                Card.HoursReveal.class);
        assertEquals(Optional.empty(), n.changeVsPreviousYearPercent());
    }

    @Test
    void thresholdsGateCards() {
        YearStats f = Fixtures.full();
        List<Card> weak = CardDeck.build(Fixtures.with(f, Duration.ofMinutes(90), 19, 1, 0), Optional.empty(), 1);
        assertFalse(has(weak, Card.Guess.class));
        assertTrue(has(weak, Card.PlayTime.class));
        assertFalse(has(weak, Card.Streak.class));
        assertFalse(has(weak, Card.ChatCard.class));
        assertFalse(has(weak, Card.Fights.class));
        List<Card> strong = CardDeck.build(Fixtures.with(f, Duration.ofHours(2), 20, 2, 1), Optional.empty(), 1);
        assertTrue(has(strong, Card.Guess.class));
        assertTrue(has(strong, Card.Streak.class));
        assertTrue(has(strong, Card.ChatCard.class));
        assertTrue(has(strong, Card.Fights.class));
        assertFalse(has(CardDeck.build(Fixtures.with(f, Duration.ofMinutes(59), 0, 0, 0), Optional.empty(), 1),
                Card.PlayTime.class));
    }

    @Test
    void emptyWorldStatsHaveNoBlocksOrFights() {
        List<Card> deck = CardDeck.build(Fixtures.with(Fixtures.full(), Duration.ofHours(5), 0, 0, 0),
                Optional.of(Fixtures.world(0, 0, 0, 0)), 1);
        assertFalse(has(deck, Card.Blocks.class));
        assertFalse(has(deck, Card.Fights.class));
    }

    @Test
    void fightsCombineWorldAndServer() {
        Card.Fights f = find(CardDeck.build(Fixtures.full(), Optional.of(Fixtures.world(1, 1, 3, 40)), 1),
                Card.Fights.class);
        assertEquals(7, f.deaths());
        assertEquals(OptionalLong.of(20), f.heartsDealt());
        assertEquals(OptionalLong.of(10), f.heartsTaken());
        assertEquals(OptionalLong.of(40), f.mobKills());
        assertEquals(OptionalLong.of(2), f.playersKilled());
        assertEquals(Optional.of("Zombie"), f.topKiller());
    }

    @Test
    void fightsFallBackToServerKiller() {
        WorldStats w = new WorldStats(1, 1, 0, 0, 2, 0, Optional.empty());
        Card.Fights f = find(CardDeck.build(Fixtures.full(), Optional.of(w), 1), Card.Fights.class);
        assertEquals(Optional.of("Steve"), f.topKiller());
    }

    @Test
    void longestSessionNeedsTenMinutesAndNamesWorld() {
        YearStats f = Fixtures.full();
        Session world = new Session("me", Session.Target.world(), LocalDateTime.of(2026, 1, 1, 1, 0),
                LocalDateTime.of(2026, 1, 1, 3, 0));
        Card.LongestSession c = find(CardDeck.build(withSession(f, world), Optional.empty(), 1),
                Card.LongestSession.class);
        assertEquals("Singleplayer", c.serverName());
        assertEquals(Duration.ofHours(2), c.duration());
        assertEquals(world.end(), c.end());
        Session shortS = new Session("me", Session.Target.world(), LocalDateTime.of(2026, 1, 1, 1, 0),
                LocalDateTime.of(2026, 1, 1, 1, 9));
        assertFalse(has(CardDeck.build(withSession(f, shortS), Optional.empty(), 1), Card.LongestSession.class));
        Session server = new Session("me", Session.Target.server("play.x.net"), LocalDateTime.of(2026, 1, 1, 1, 0),
                LocalDateTime.of(2026, 1, 1, 1, 10));
        assertEquals("play.x.net",
                find(CardDeck.build(withSession(f, server), Optional.empty(), 1), Card.LongestSession.class)
                        .serverName());
    }

    @Test
    void playTimeLabels() {
        assertEquals("Night Owl", Card.PlayTime.label(Fixtures.hourly(23, 100)));
        assertEquals("Night Owl", Card.PlayTime.label(Fixtures.hourly(3, 100)));
        assertEquals("Early Bird", Card.PlayTime.label(Fixtures.hourly(7, 100)));
        assertEquals("Daytime Player", Card.PlayTime.label(Fixtures.hourly(14, 100)));
        assertEquals("Daytime Player", Card.PlayTime.label(new long[24]));
        assertEquals("Evening Player", Card.PlayTime.label(Fixtures.hourly(19, 100)));
        long[] evening = {85, 72, 52, 40, 40, 40, 40, 40, 40, 40, 40, 40, 46, 51, 57, 57, 62, 78, 74, 78, 88, 94,
            100, 91};
        assertEquals("Evening Player", Card.PlayTime.label(evening));
        assertEquals(19, Card.PlayTime.peakStart(evening));
        assertTrue(Card.PlayTime.inPeak(evening, 0));
        assertTrue(Card.PlayTime.inPeak(evening, 22));
        assertFalse(Card.PlayTime.inPeak(evening, 12));
        long[] late = evening.clone();
        late[1] = 100;
        late[2] = 100;
        assertEquals("Night Owl", Card.PlayTime.label(late));
        assertEquals("Around the Clock", Card.PlayTime.label(Fixtures.evenly()));
    }

    @Test
    void summaryAndTypeCarryHighlights() {
        List<Card> deck = CardDeck.build(Fixtures.full(), Optional.empty(), 1);
        Card.Summary s = (Card.Summary) deck.get(deck.size() - 1);
        assertEquals(214, s.totalHours());
        assertEquals(Optional.of("a.net"), s.topServer());
        assertEquals(9, s.longestStreakDays());
        YearStats full = Fixtures.full();
        assertEquals(full.uniqueServers(), s.extras().servers());
        assertEquals(full.messagesSent(), s.extras().messagesSent());
        assertEquals(full.topWord(), s.extras().topWord());
        assertEquals(OptionalLong.empty(), s.extras().blocksMined());
        assertEquals(full.longestSession().map(Session::duration),
                s.extras().longestSession());
        assertArrayEquals(full.secondsByHour(), s.extras().secondsByHour());
        Card.Title t = (Card.Title) deck.get(0);
        assertEquals(2026, t.year());
        Card.Type type = (Card.Type) deck.get(deck.size() - 2);
        assertEquals(s.playerType(), type.playerType());
    }

    @Test
    void serverOnlyFightsShowNoWorldSlots() {
        YearStats y = Fixtures.with(Fixtures.full(), Duration.ofHours(5), 0, 0, 3);
        Card.Fights f = find(CardDeck.build(y, Optional.empty(), 1), Card.Fights.class);
        assertEquals(3, f.deaths());
        assertEquals(OptionalLong.empty(), f.heartsDealt());
        assertEquals(OptionalLong.empty(), f.heartsTaken());
        assertEquals(OptionalLong.empty(), f.mobKills());
        assertEquals(OptionalLong.of(2), f.playersKilled());
        assertEquals(Optional.of("Steve"), f.topKiller());
    }

    @Test
    void worldOnlyFightsHaveNoPlayersKilled() {
        YearStats f = Fixtures.full();
        YearStats y = new YearStats(2026, f.totalTime(), 0, List.of(), Optional.empty(), f.longestSession(),
                f.secondsByHour(), 0, f.firstDay(), 0, Optional.empty(), 0, 0, Optional.empty(), false, Duration.ZERO);
        Card.Fights c = find(CardDeck.build(y, Optional.of(Fixtures.world(10, 10, 2, 0)), 1), Card.Fights.class);
        assertEquals(2, c.deaths());
        assertEquals(OptionalLong.of(20), c.heartsDealt());
        assertEquals(OptionalLong.of(10), c.heartsTaken());
        assertEquals(OptionalLong.of(0), c.mobKills());
        assertEquals(OptionalLong.empty(), c.playersKilled());
    }

    @Test
    void summaryTopServerIsMostHoursNotMostDays() {
        List<com.wrapped.stats.ServerStat> servers = List.of(Fixtures.server("hours.net", 80),
                new com.wrapped.stats.ServerStat("days.net", Duration.ofHours(20), 30, 30,
                        java.time.LocalDate.of(2026, 1, 1)));
        YearStats y = new YearStats(2026, Duration.ofHours(100), 2, servers, Optional.of(servers.get(1)),
                Optional.empty(), Fixtures.evenly(), 0, Optional.empty(), 0, Optional.empty(), 0, 0, Optional.empty(),
                false, Duration.ZERO);
        List<Card> deck = CardDeck.build(y, Optional.empty(), 1);
        assertEquals(Optional.of("hours.net"), find(deck, Card.Summary.class).topServer());
        assertEquals("hours.net", find(deck, Card.TopServer.class).server().host());
        assertEquals(PlayerType.LOYAL_REGULAR, find(deck, Card.Summary.class).playerType());
    }

    @Test
    void zeroChangeSaysSameAs() {
        Card.HoursReveal r = find(CardDeck.build(withTimes(Fixtures.full(), 100, true, Duration.ofHours(100)),
                Optional.empty(), 1), Card.HoursReveal.class);
        assertEquals("same as 2025", r.changeText().orElseThrow());
    }
}
