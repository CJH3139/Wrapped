package com.wrapped.stats;

import com.wrapped.chat.WordCounter;
import com.wrapped.sessions.HostNormalizer;
import com.wrapped.sessions.Session;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class YearStatsBuilder {
    private YearStatsBuilder() {
    }

    public static final Duration YEAR_MIN = Duration.ofHours(1);

    public static YearStats build(int year, String account, Collection<FileRecord> records, LocalDate today) {
        List<Session> sessions = sessions(year, account, records);
        sessions.sort(Comparator.comparing(Session::start));
        Duration total = Duration.ZERO;
        long[] hours = new long[24];
        SortedSet<LocalDate> days = new TreeSet<>();
        Session longest = null;
        Map<Session.Target, Session> chains = new HashMap<>();
        Map<String, List<Session>> byServer = new HashMap<>();
        for (Session s : sessions) {
            total = total.plus(s.duration());
            addHours(hours, s.start(), s.end());
            days.add(s.start().toLocalDate());
            Session chain = chains.get(s.target());
            if (chain != null && s.continued() && !s.end().isBefore(chain.end())
                    && !chain.end().toLocalDate().isBefore(s.start().toLocalDate().minusDays(1))) {
                chain = new Session(chain.account(), chain.target(), chain.start(), s.end());
            } else {
                chain = s;
            }
            chains.put(s.target(), chain);
            if (longest == null || chain.duration().compareTo(longest.duration()) > 0) {
                longest = chain;
            }
            if (s.target().kind() == Session.Kind.SERVER && !HostNormalizer.isLocal(s.target().id())) {
                byServer.computeIfAbsent(s.target().id(), k -> new ArrayList<>()).add(s);
            }
        }
        List<ServerStat> servers = new ArrayList<>();
        for (Map.Entry<String, List<Session>> e : byServer.entrySet()) {
            Duration time = Duration.ZERO;
            Set<LocalDate> active = new HashSet<>();
            LocalDate first = null;
            int joins = 0;
            for (Session s : e.getValue()) {
                if (!s.continued()) {
                    joins++;
                }
                time = time.plus(s.duration());
                LocalDate d = s.start().toLocalDate();
                active.add(d);
                if (first == null || d.isBefore(first)) {
                    first = d;
                }
            }
            servers.add(new ServerStat(e.getKey(), time, Math.max(1, joins), active.size(), first));
        }
        servers.sort(Comparator.comparing(ServerStat::time).reversed().thenComparing(ServerStat::host));
        Optional<ServerStat> mostActive = servers.stream().min(
                Comparator.comparingInt(ServerStat::activeDays).reversed()
                        .thenComparing(Comparator.comparing(ServerStat::time).reversed())
                        .thenComparing(ServerStat::host));

        int messages = 0;
        int deaths = 0;
        int kills = 0;
        Map<String, Integer> words = new HashMap<>();
        Map<String, Integer> killers = new HashMap<>();
        for (FileRecord r : records) {
            ChatSummary c = r.chat().getOrDefault(account, Map.of()).get(year);
            if (c == null) {
                continue;
            }
            messages += c.messagesSent();
            deaths += c.deaths();
            kills += c.kills();
            c.wordCounts().forEach((k, v) -> words.merge(k, v, Integer::sum));
            c.killers().forEach((k, v) -> killers.merge(k, v, Integer::sum));
        }

        List<Session> previous = sessions(year - 1, account, records);
        if (year == today.getYear()) {
            LocalDate cutoff = today.withYear(year - 1);
            previous.removeIf(s -> s.start().toLocalDate().isAfter(cutoff));
        }
        Duration previousTime = Duration.ZERO;
        for (Session s : previous) {
            previousTime = previousTime.plus(s.duration());
        }

        return new YearStats(year, total, servers.size(), List.copyOf(servers), mostActive, Optional.ofNullable(longest), hours,
                streak(days), days.isEmpty() ? Optional.empty() : Optional.of(days.first()), messages, topWord(words), deaths,
                kills, top(killers), !previous.isEmpty(), previousTime);
    }

    public static SortedSet<Integer> years(String account, Collection<FileRecord> records) {
        Map<Integer, Duration> time = new HashMap<>();
        for (FileRecord r : records) {
            for (Session s : r.sessions()) {
                if (s.account().equals(account)) {
                    time.merge(s.start().getYear(), s.duration(), Duration::plus);
                }
            }
        }
        SortedSet<Integer> out = new TreeSet<>();
        time.forEach((year, d) -> {
            if (d.compareTo(YEAR_MIN) >= 0) {
                out.add(year);
            }
        });
        return out;
    }

    private static List<Session> sessions(int year, String account, Collection<FileRecord> records) {
        List<Session> all = new ArrayList<>();
        for (FileRecord r : records) {
            for (Session s : r.sessions()) {
                if (s.account().equals(account) && s.start().getYear() == year) {
                    all.add(s);
                }
            }
        }
        all.sort(Comparator.comparing(Session::start).thenComparing(Session::end, Comparator.reverseOrder()));
        List<Session> out = new ArrayList<>();
        LocalDateTime covered = null;
        for (Session s : all) {
            if (covered != null && !s.end().isAfter(covered)) {
                continue;
            }
            if (covered != null && s.start().isBefore(covered)) {
                s = new Session(s.account(), s.target(), covered, s.end(), s.continued());
            }
            out.add(s);
            covered = s.end();
        }
        return out;
    }

    private static void addHours(long[] hours, LocalDateTime start, LocalDateTime end) {
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            LocalDateTime next = cursor.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            if (next.isAfter(end)) {
                next = end;
            }
            hours[cursor.getHour()] += Duration.between(cursor, next).getSeconds();
            cursor = next;
        }
    }

    private static int streak(Set<LocalDate> sortedDays) {
        int best = 0;
        int run = 0;
        LocalDate prev = null;
        for (LocalDate d : sortedDays) {
            run = prev != null && prev.plusDays(1).equals(d) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    private static Optional<String> topWord(Map<String, Integer> counts) {
        Map<String, Integer> kept = new HashMap<>();
        counts.forEach((k, v) -> {
            if (WordCounter.isCountable(k)) {
                kept.put(k, v);
            }
        });
        return top(kept);
    }

    private static Optional<String> top(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .min(Comparator.<Map.Entry<String, Integer>>comparingInt(e -> -e.getValue()).thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey);
    }
}
