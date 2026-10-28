package com.wrapped.stats;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.logs.LogEvent;
import com.wrapped.sessions.Session;
import com.wrapped.sessions.SessionBuilder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class YearStatsBuilderTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private static Session server(String account, String host, LocalDateTime start, LocalDateTime end) {
        return new Session(account, Session.Target.server(host), start, end);
    }

    private static LocalDateTime at(int y, int mo, int d, int h, int mi) {
        return LocalDateTime.of(y, mo, d, h, mi);
    }

    private static FileRecord record(String name, List<Session> sessions, Map<String, Map<Integer, ChatSummary>> chat) {
        return new FileRecord(name, false, name, LocalDate.of(2026, 1, 1), sessions, chat);
    }

    private static FileRecord record(Session... sessions) {
        return record("f", List.of(sessions), Map.of());
    }

    @Test
    void onlyCurrentAccount() {
        ChatSummary mine = new ChatSummary(3, Map.of("diamonds", 2), 1, 0, Map.of("Zombie", 1));
        ChatSummary theirs = new ChatSummary(50, Map.of("creeper", 40), 9, 9, Map.of("Skeleton", 9));
        FileRecord r = record("f",
                List.of(server("iCJH", "minehut.com", at(2026, 3, 1, 10, 0), at(2026, 3, 1, 12, 0)),
                        server("OffByOnee", "minehut.com", at(2026, 3, 1, 10, 0), at(2026, 3, 1, 20, 0))),
                Map.of("iCJH", Map.of(2026, mine), "OffByOnee", Map.of(2026, theirs)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(Duration.ofHours(2), s.totalTime());
        assertEquals(3, s.messagesSent());
        assertEquals(Optional.of("diamonds"), s.topWord());
        assertEquals(1, s.serverDeaths());
        assertEquals(0, s.serverKills());
        assertEquals(Optional.of("Zombie"), s.topServerKiller());
        assertEquals(1, s.serversByTime().size());
        assertEquals(java.util.TreeSet.class, YearStatsBuilder.years("iCJH", List.of(r)).getClass());
        assertEquals(List.of(2026), List.copyOf(YearStatsBuilder.years("iCJH", List.of(r))));
        assertTrue(YearStatsBuilder.years("nobody", List.of(r)).isEmpty());
    }

    @Test
    void localNotRanked() {
        FileRecord r = record(
                server("iCJH", "localhost", at(2026, 3, 1, 0, 0), at(2026, 3, 1, 10, 0)),
                new Session("iCJH", Session.Target.world(), at(2026, 3, 2, 0, 0), at(2026, 3, 2, 1, 0)),
                server("iCJH", "minehut.com", at(2026, 3, 3, 0, 0), at(2026, 3, 3, 2, 0)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(1, s.uniqueServers());
        assertEquals("minehut.com", s.serversByTime().get(0).host());
        assertEquals(Optional.of("minehut.com"), s.mostActive().map(ServerStat::host));
        assertEquals(Duration.ofHours(13), s.totalTime());
        assertEquals(10, s.longestSession().orElseThrow().duration().toHours());
    }

    @Test
    void allLocalHostSpellingsExcluded() {
        FileRecord r = record(
                server("iCJH", "127.0.0.1", at(2026, 3, 1, 0, 0), at(2026, 3, 1, 1, 0)),
                server("iCJH", "192.168.1.5", at(2026, 3, 2, 0, 0), at(2026, 3, 2, 1, 0)),
                server("iCJH", "10.0.0.2", at(2026, 3, 3, 0, 0), at(2026, 3, 3, 1, 0)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(0, s.uniqueServers());
        assertTrue(s.serversByTime().isEmpty());
        assertTrue(s.mostActive().isEmpty());
        assertEquals(Duration.ofHours(3), s.totalTime());
    }

    @Test
    void hostSpellingsCountAsOneServer() {
        List<LogEvent> events = List.of(
                new LogEvent.UserSet(LocalTime.of(9, 0), "iCJH"),
                new LogEvent.ServerConnect(LocalTime.of(10, 0), "Minehut.com", 25565),
                new LogEvent.Leave(LocalTime.of(11, 0)),
                new LogEvent.ServerConnect(LocalTime.of(12, 0), "minehut.com.", 25565),
                new LogEvent.Leave(LocalTime.of(13, 0)),
                new LogEvent.ServerConnect(LocalTime.of(14, 0), "minehut.com", 25565),
                new LogEvent.Leave(LocalTime.of(15, 0)));
        List<Session> sessions = SessionBuilder.build(LocalDate.of(2026, 3, 1), events, Optional.empty());
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(record("f", sessions, Map.of())), TODAY);
        assertEquals(1, s.uniqueServers());
        assertEquals("minehut.com", s.serversByTime().get(0).host());
        assertEquals(3, s.serversByTime().get(0).joins());
        assertEquals(1, s.serversByTime().get(0).activeDays());
        assertEquals(Duration.ofHours(3), s.serversByTime().get(0).time());
    }

    @Test
    void sessionCountsInStartYear() {
        FileRecord r = record(server("iCJH", "minehut.com", at(2025, 12, 31, 23, 0), at(2026, 1, 1, 1, 0)));
        YearStats y2025 = YearStatsBuilder.build(2025, "iCJH", List.of(r), TODAY);
        YearStats y2026 = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(Duration.ofHours(2), y2025.totalTime());
        assertEquals(Duration.ZERO, y2026.totalTime());
        assertEquals(Optional.empty(), y2026.firstDay());
        assertEquals(Optional.of(LocalDate.of(2025, 12, 31)), y2025.firstDay());
    }

    @Test
    void streak() {
        FileRecord r = record(
                server("iCJH", "minehut.com", at(2026, 3, 1, 10, 0), at(2026, 3, 1, 11, 0)),
                server("iCJH", "minehut.com", at(2026, 3, 2, 10, 0), at(2026, 3, 2, 11, 0)),
                server("iCJH", "minehut.com", at(2026, 3, 3, 10, 0), at(2026, 3, 3, 11, 0)),
                server("iCJH", "minehut.com", at(2026, 3, 5, 10, 0), at(2026, 3, 5, 11, 0)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(3, s.longestStreakDays());
        assertEquals(Optional.of(LocalDate.of(2026, 3, 1)), s.firstDay());
        assertEquals(4, s.mostActive().orElseThrow().activeDays());
    }

    @Test
    void hourBuckets() {
        FileRecord r = record(server("iCJH", "minehut.com", at(2026, 3, 1, 22, 30), at(2026, 3, 1, 23, 30)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        long[] expected = new long[24];
        expected[22] = 1800;
        expected[23] = 1800;
        assertArrayEquals(expected, s.secondsByHour());
    }

    @Test
    void hourBucketsAcrossMidnight() {
        FileRecord r = record(server("iCJH", "minehut.com", at(2026, 3, 1, 23, 30), at(2026, 3, 2, 0, 30)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        long[] expected = new long[24];
        expected[23] = 1800;
        expected[0] = 1800;
        assertArrayEquals(expected, s.secondsByHour());
    }

    @Test
    void previousYear() {
        FileRecord r = record(
                server("iCJH", "minehut.com", at(2025, 5, 1, 10, 0), at(2025, 5, 1, 13, 0)),
                server("iCJH", "minehut.com", at(2026, 5, 1, 10, 0), at(2026, 5, 1, 11, 0)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertTrue(s.hasPreviousYear());
        assertEquals(Duration.ofHours(3), s.previousYearTime());
        YearStats first = YearStatsBuilder.build(2025, "iCJH", List.of(r), TODAY);
        assertFalse(first.hasPreviousYear());
        assertEquals(Duration.ZERO, first.previousYearTime());
    }

    @Test
    void chatMergesAcrossFiles() {
        FileRecord a = record("a", List.of(), Map.of("iCJH", Map.of(2026,
                new ChatSummary(2, Map.of("alpha", 1, "beta", 1), 1, 1, Map.of("Zombie", 1)))));
        FileRecord b = record("b", List.of(), Map.of("iCJH", Map.of(2026,
                new ChatSummary(1, Map.of("beta", 1), 2, 0, Map.of("Zombie", 1, "Husk", 1)))));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(a, b), TODAY);
        assertEquals(3, s.messagesSent());
        assertEquals(Optional.of("beta"), s.topWord());
        assertEquals(3, s.serverDeaths());
        assertEquals(1, s.serverKills());
        assertEquals(Optional.of("Zombie"), s.topServerKiller());
        assertTrue(YearStatsBuilder.years("iCJH", List.of(a, b)).isEmpty());
    }

    private static ChatSummary chatOf(String... texts) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("[10:00:00] [main/INFO]: Setting user: iCJH");
        for (String t : texts) {
            lines.add("[10:01:00] [Render thread/INFO]: [CHAT] " + t);
        }
        return FileRecords.parse("f", false, "fp", LocalDate.of(2026, 3, 1), lines).chat().get("iCJH").get(2026);
    }

    @Test
    void prefixedPlayerChatIsNotADeath() {
        ChatSummary c = chatOf("Bob: iCJH died lol", "Guild > Bob: iCJH died", "VIP Bob: iCJH died",
                "★ Bob » iCJH was slain by Zombie");
        assertEquals(0, c.deaths());
        assertEquals(Map.of(), c.killers());
    }

    @Test
    void systemLinesStillCount() {
        ChatSummary c = chatOf("iCJH was slain by Bob using [Blade: X]", "Zombie was slain by iCJH");
        assertEquals(1, c.deaths());
        assertEquals(Map.of("Bob", 1), c.killers());
        assertEquals(1, c.kills());
    }

    @Test
    void fileRecordIsImmutable() {
        java.util.ArrayList<Session> list = new java.util.ArrayList<>();
        java.util.HashMap<Integer, ChatSummary> years = new java.util.HashMap<>();
        java.util.HashMap<String, Map<Integer, ChatSummary>> chat = new java.util.HashMap<>();
        chat.put("iCJH", years);
        FileRecord r = new FileRecord("f", false, "fp", LocalDate.of(2026, 1, 1), list, chat);
        list.add(server("iCJH", "a", at(2026, 1, 1, 1, 0), at(2026, 1, 1, 2, 0)));
        years.put(2026, new ChatSummary(1, Map.of(), 0, 0, Map.of()));
        assertTrue(r.sessions().isEmpty());
        assertTrue(r.chat().get("iCJH").isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> r.sessions().add(null));
    }

    @Test
    void localSessionsFeedHoursAndStreak() {
        FileRecord r = record(
                server("iCJH", "localhost", at(2026, 3, 1, 5, 0), at(2026, 3, 1, 6, 0)),
                new Session("iCJH", Session.Target.world(), at(2026, 3, 2, 5, 0), at(2026, 3, 2, 6, 0)),
                server("iCJH", "192.168.1.5", at(2026, 3, 3, 5, 0), at(2026, 3, 3, 6, 0)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(10800, s.secondsByHour()[5]);
        assertEquals(3, s.longestStreakDays());
        assertEquals(0, s.uniqueServers());
    }

    @Test
    void fileRecordsParsesChat() {
        List<String> lines = List.of(
                "[10:00:00] [main/INFO]: Setting user: iCJH",
                "[10:00:05] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[10:01:00] [Render thread/INFO]: [CHAT] <iCJH> hello diamonds",
                "[10:01:10] [Render thread/INFO]: [CHAT] <Bob> iCJH died lol",
                "[10:01:20] [Render thread/INFO]: [CHAT] iCJH was slain by Zombie",
                "[10:30:00] [Render thread/INFO]: Stopping!");
        FileRecord r = FileRecords.parse("2026-03-01-1.log.gz", true, "fp", LocalDate.of(2026, 3, 1), lines);
        ChatSummary c = r.chat().get("iCJH").get(2026);
        assertEquals(1, c.messagesSent());
        assertEquals(1, c.deaths());
        assertEquals(Map.of("Zombie", 1), c.killers());
        assertEquals(0, c.kills());
        assertEquals(Map.of("hello", 1, "diamonds", 1), c.wordCounts());
        assertEquals(1, r.sessions().size());
        assertEquals("fp", r.fingerprint());
        assertTrue(r.latest());
    }

    @Test
    void currentYearComparesAgainstSamePointLastYear() {
        FileRecord r = record(
                server("iCJH", "minehut.com", at(2025, 3, 10, 10, 0), at(2025, 3, 10, 12, 0)),
                server("iCJH", "minehut.com", at(2025, 3, 15, 10, 0), at(2025, 3, 15, 11, 0)),
                server("iCJH", "minehut.com", at(2025, 6, 1, 10, 0), at(2025, 6, 1, 15, 0)),
                server("iCJH", "minehut.com", at(2026, 3, 1, 10, 0), at(2026, 3, 1, 13, 0)));
        LocalDate today = LocalDate.of(2026, 3, 15);
        YearStats now = YearStatsBuilder.build(2026, "iCJH", List.of(r), today);
        assertEquals(Duration.ofHours(3), now.previousYearTime());
        assertTrue(now.hasPreviousYear());
        YearStats past = YearStatsBuilder.build(2026, "iCJH", List.of(r), LocalDate.of(2027, 1, 5));
        assertEquals(Duration.ofHours(8), past.previousYearTime());
        YearStats early = YearStatsBuilder.build(2026, "iCJH", List.of(r), LocalDate.of(2026, 2, 1));
        assertFalse(early.hasPreviousYear());
        assertEquals(Duration.ZERO, early.previousYearTime());
    }

    @Test
    void topWordIgnoresStopwordsAndApostrophesInStoredCounts() {
        FileRecord r = record("f", List.of(), Map.of("iCJH", Map.of(2026,
                new ChatSummary(9, Map.of("dont", 9, "don", 8, "it's", 7, "would", 6, "diamonds", 1), 0, 0, Map.of()))));
        assertEquals(Optional.of("diamonds"), YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY).topWord());
    }

    @Test
    void yearsNeedAnHourOfSessions() {
        FileRecord r = record(
                server("iCJH", "minehut.com", at(2024, 3, 1, 10, 0), at(2024, 3, 1, 10, 59)),
                server("iCJH", "minehut.com", at(2025, 3, 1, 10, 0), at(2025, 3, 1, 10, 30)),
                new Session("iCJH", Session.Target.world(), at(2025, 4, 1, 10, 0), at(2025, 4, 1, 10, 30)),
                server("iCJH", "minehut.com", at(2026, 3, 1, 10, 0), at(2026, 3, 1, 12, 0)));
        assertEquals(List.of(2025, 2026), List.copyOf(YearStatsBuilder.years("iCJH", List.of(r))));
    }

    @Test
    void continuedSessionIsNotAJoinAndExtendsTheLongestSession() {
        FileRecord r = record(
                server("iCJH", "minehut.com", at(2026, 3, 1, 22, 0), at(2026, 3, 1, 23, 59)),
                new Session("iCJH", Session.Target.server("minehut.com"), at(2026, 3, 2, 0, 0), at(2026, 3, 2, 2, 0), true),
                server("iCJH", "minehut.com", at(2026, 3, 2, 10, 0), at(2026, 3, 2, 13, 0)));
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(r), TODAY);
        assertEquals(2, s.serversByTime().get(0).joins());
        assertEquals(Duration.ofHours(4), s.longestSession().orElseThrow().duration());
        assertEquals(at(2026, 3, 2, 2, 0), s.longestSession().orElseThrow().end());
    }

    @Test
    void chatWithoutTheAccountNameIsSkippedCheaply() {
        ChatSummary c = chatOf("<iCJH> hello", "<Bob> nothing here", "Zombie was slain by Bob");
        assertEquals(1, c.messagesSent());
        assertEquals(0, c.deaths());
        assertEquals(0, c.kills());
    }

    @Test
    void overlappingInstancesCountOnce() {
        FileRecord a = record("a", List.of(server("iCJH", "a.net", at(2026, 3, 1, 10, 0), at(2026, 3, 1, 12, 0))), Map.of());
        FileRecord b = record("b", List.of(server("iCJH", "b.net", at(2026, 3, 1, 11, 0), at(2026, 3, 1, 13, 0)),
                server("iCJH", "c.net", at(2026, 3, 1, 11, 15), at(2026, 3, 1, 11, 45))), Map.of());
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(a, b), TODAY);
        assertEquals(Duration.ofHours(3), s.totalTime());
        assertEquals(Duration.ofHours(2), s.serversByTime().get(0).time());
        assertEquals(Duration.ofHours(1), s.serversByTime().get(1).time());
        assertEquals(2, s.uniqueServers());
    }

    @Test
    void midnightChainSurvivesOtherInstance() {
        FileRecord day1 = record("a1", List.of(server("iCJH", "a.net", at(2026, 3, 1, 20, 0), at(2026, 3, 1, 23, 50))), Map.of());
        FileRecord other = record("b", List.of(server("iCJH", "b.net", at(2026, 3, 1, 23, 50), at(2026, 3, 1, 23, 55))), Map.of());
        FileRecord day2 = record("a2", List.of(new Session("iCJH", Session.Target.server("a.net"),
                at(2026, 3, 2, 0, 0), at(2026, 3, 2, 4, 0), true)), Map.of());
        YearStats s = YearStatsBuilder.build(2026, "iCJH", List.of(day2, other, day1), TODAY);
        assertEquals(Duration.ofHours(8), s.longestSession().orElseThrow().duration());
        assertEquals(at(2026, 3, 1, 20, 0), s.longestSession().orElseThrow().start());
    }
}
