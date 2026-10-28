package com.wrapped.cards;

import com.wrapped.sessions.Session;
import com.wrapped.stats.ServerStat;
import com.wrapped.stats.YearStats;
import com.wrapped.world.WorldStats;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class CardDeck {
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration GUESS_MIN = Duration.ofHours(2);
    private static final Duration SESSION_MIN = Duration.ofMinutes(10);
    private static final int CHAT_MIN = 20;
    private static final int STREAK_MIN = 2;
    private static final long DAMAGE_PER_HEART = 20;

    private CardDeck() {
    }

    public static List<Card> build(YearStats stats, Optional<WorldStats> world, long seed) {
        List<Card> deck = new ArrayList<>();
        long hours = Math.round(stats.totalTime().toSeconds() / 3600.0);
        PlayerType type = PlayerType.classify(stats, world);

        deck.add(new Card.Title(stats.year()));
        if (stats.totalTime().compareTo(GUESS_MIN) >= 0) {
            int real = (int) hours;
            deck.add(new Card.Guess(real, GuessOptions.of(real, seed)));
        }
        deck.add(new Card.HoursReveal(hours, Relatable.hours(stats.totalTime()), change(stats), stats.year() - 1));
        if (stats.uniqueServers() >= 2) {
            deck.add(new Card.ServerCount(stats.uniqueServers()));
        }
        List<ServerStat> ranked = stats.serversByTime();
        if (ranked.size() >= 3) {
            List<ServerStat> shown = new ArrayList<>(ranked.subList(1, Math.min(5, ranked.size())));
            Collections.reverse(shown);
            deck.add(new Card.Countdown(shown));
        }
        if (stats.uniqueServers() >= 1 && !ranked.isEmpty()) {
            deck.add(new Card.TopServer(ranked.get(0)));
        }
        stats.longestSession().filter(s -> s.duration().compareTo(SESSION_MIN) >= 0)
                .ifPresent(s -> deck.add(new Card.LongestSession(s.duration(), serverName(s), s.end())));
        if (stats.totalTime().compareTo(ONE_HOUR) >= 0) {
            deck.add(new Card.PlayTime(stats.secondsByHour(), Card.PlayTime.label(stats.secondsByHour())));
        }
        if (stats.longestStreakDays() >= STREAK_MIN) {
            deck.add(new Card.Streak(stats.longestStreakDays()));
        }
        if (stats.messagesSent() >= CHAT_MIN) {
            deck.add(new Card.ChatCard(stats.messagesSent(), stats.topWord()));
        }
        world.filter(w -> w.blocksMined() + w.blocksPlaced() > 0)
                .ifPresent(w -> deck.add(new Card.Blocks(w.blocksMined(), w.blocksPlaced())));
        long worldDeaths = world.map(WorldStats::deaths).orElse(0L);
        long worldKills = world.map(WorldStats::mobKills).orElse(0L);
        if (worldDeaths > 0 || worldKills > 0 || stats.serverDeaths() > 0) {
            Optional<String> killer = world.flatMap(WorldStats::topKiller).or(stats::topServerKiller);
            deck.add(new Card.Fights(worldDeaths + stats.serverDeaths(),
                    world.map(w -> OptionalLong.of(w.damageDealt() / DAMAGE_PER_HEART)).orElse(OptionalLong.empty()),
                    world.map(w -> OptionalLong.of(w.damageTaken() / DAMAGE_PER_HEART)).orElse(OptionalLong.empty()),
                    world.map(w -> OptionalLong.of(w.mobKills())).orElse(OptionalLong.empty()),
                    stats.serverKills() > 0 ? OptionalLong.of(stats.serverKills()) : OptionalLong.empty(), killer));
        }
        deck.add(new Card.Type(type));
        Card.Summary.Extras extras = new Card.Summary.Extras(stats.uniqueServers(), stats.messagesSent(),
                stats.topWord(), world.map(w -> OptionalLong.of(w.blocksMined())).orElse(OptionalLong.empty()),
                world.map(w -> OptionalLong.of(w.mobKills())).orElse(OptionalLong.empty()),
                worldDeaths + stats.serverDeaths(), stats.longestSession().map(Session::duration),
                stats.secondsByHour());
        deck.add(new Card.Summary(hours, ranked.stream().findFirst().map(ServerStat::host), stats.longestStreakDays(),
                type, extras));
        return List.copyOf(deck);
    }

    private static Optional<Integer> change(YearStats stats) {
        if (!stats.hasPreviousYear() || stats.previousYearTime().compareTo(ONE_HOUR) < 0) {
            return Optional.empty();
        }
        double prev = stats.previousYearTime().toSeconds();
        double cur = stats.totalTime().toSeconds();
        return Optional.of((int) Math.round((cur - prev) / prev * 100.0));
    }

    private static String serverName(Session s) {
        return s.target().kind() == Session.Kind.WORLD ? "Singleplayer" : s.target().id();
    }
}
