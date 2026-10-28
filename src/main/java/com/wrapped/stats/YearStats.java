package com.wrapped.stats;

import com.wrapped.sessions.Session;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record YearStats(int year, Duration totalTime, int uniqueServers, List<ServerStat> serversByTime,
        Optional<ServerStat> mostActive, Optional<Session> longestSession, long[] secondsByHour, int longestStreakDays,
        Optional<LocalDate> firstDay, int messagesSent, Optional<String> topWord, int serverDeaths, int serverKills,
        Optional<String> topServerKiller, boolean hasPreviousYear, Duration previousYearTime) {
}
