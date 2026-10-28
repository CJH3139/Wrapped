package com.wrapped.cards;

import com.wrapped.stats.ServerStat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public sealed interface Card permits Card.Title, Card.Guess, Card.HoursReveal, Card.ServerCount, Card.Countdown,
        Card.TopServer, Card.LongestSession, Card.PlayTime, Card.Streak, Card.ChatCard, Card.Blocks, Card.Fights,
        Card.Type, Card.Summary {

    record Title(int year) implements Card {
    }

    record Guess(int realHours, List<Integer> options) implements Card {
        public Guess {
            options = List.copyOf(options);
        }
    }

    record HoursReveal(long hours, String relatable, Optional<Integer> changeVsPreviousYearPercent, int previousYear)
            implements Card {
        public Optional<String> changeText() {
            return changeVsPreviousYearPercent.map(p -> p == 0 ? "same as " + previousYear
                    : (p > 0 ? "up " : "down ") + Math.abs(p) + "% from " + previousYear);
        }
    }

    record ServerCount(int count) implements Card {
    }

    record Countdown(List<ServerStat> ranked) implements Card {
        public Countdown {
            ranked = List.copyOf(ranked);
        }
    }

    record TopServer(ServerStat server) implements Card {
    }

    record LongestSession(Duration duration, String serverName, LocalDateTime end) implements Card {
    }

    record PlayTime(long[] secondsByHour, String label) implements Card {
        public static final String NIGHT_OWL = "Night Owl";
        public static final String EARLY_BIRD = "Early Bird";
        public static final String DAYTIME = "Daytime Player";
        public static final String EVENING = "Evening Player";
        public static final String ALL_DAY = "Around the Clock";
        private static final int WINDOW = 6;
        private static final double PEAK_MIN_SHARE = 0.3;

        public PlayTime {
            secondsByHour = secondsByHour.clone();
        }

        @Override
        public long[] secondsByHour() {
            return secondsByHour.clone();
        }

        public static int peakStart(long[] secondsByHour) {
            int n = secondsByHour.length;
            int best = 0;
            long bestSum = -1;
            for (int s = 0; s < n; s++) {
                long sum = 0;
                for (int k = 0; k < WINDOW; k++) {
                    sum += secondsByHour[(s + k) % n];
                }
                if (sum > bestSum) {
                    bestSum = sum;
                    best = s;
                }
            }
            return best;
        }

        public static boolean inPeak(long[] secondsByHour, int hour) {
            int n = secondsByHour.length;
            return n > 0 && Math.floorMod(hour - peakStart(secondsByHour), n) < WINDOW;
        }

        public static String label(long[] secondsByHour) {
            int n = secondsByHour.length;
            int start = peakStart(secondsByHour);
            long total = 0;
            for (long v : secondsByHour) {
                total += v;
            }
            long sum = 0;
            double weighted = 0;
            for (int k = 0; k < WINDOW && n > 0; k++) {
                long v = secondsByHour[(start + k) % n];
                sum += v;
                weighted += (double) k * v;
            }
            if (sum <= 0) {
                return DAYTIME;
            }
            if (sum < total * PEAK_MIN_SHARE) {
                return ALL_DAY;
            }
            double center = (start + weighted / sum) % 24;
            if (center >= 22 || center < 4) {
                return NIGHT_OWL;
            }
            if (center < 10) {
                return EARLY_BIRD;
            }
            if (center < 16) {
                return DAYTIME;
            }
            return EVENING;
        }
    }

    record Streak(int days) implements Card {
    }

    record ChatCard(int messagesSent, Optional<String> topWord) implements Card {
    }

    record Blocks(long mined, long placed) implements Card {
    }

    record Fights(long deaths, OptionalLong heartsDealt, OptionalLong heartsTaken, OptionalLong mobKills,
            OptionalLong playersKilled, Optional<String> topKiller) implements Card {
    }

    record Type(PlayerType playerType) implements Card {
    }

    record Summary(long totalHours, Optional<String> topServer, int longestStreakDays, PlayerType playerType,
            Extras extras) implements Card {
        public Summary(long totalHours, Optional<String> topServer, int longestStreakDays, PlayerType playerType) {
            this(totalHours, topServer, longestStreakDays, playerType, Extras.NONE);
        }

        public record Extras(int servers, int messagesSent, Optional<String> topWord, OptionalLong blocksMined,
                OptionalLong mobKills, long deaths, Optional<Duration> longestSession, long[] secondsByHour) {
            public static final Extras NONE = new Extras(0, 0, Optional.empty(), OptionalLong.empty(),
                    OptionalLong.empty(), 0, Optional.empty(), new long[24]);

            public Extras {
                secondsByHour = secondsByHour.clone();
            }

            @Override
            public long[] secondsByHour() {
                return secondsByHour.clone();
            }
        }
    }
}
