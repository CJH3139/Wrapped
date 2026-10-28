package com.wrapped.cards;

import com.wrapped.sessions.Session;
import com.wrapped.stats.ServerStat;
import com.wrapped.stats.YearStats;
import com.wrapped.world.WorldStats;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

final class Fixtures {
    private Fixtures() {
    }

    static ServerStat server(String host, long hours) {
        return new ServerStat(host, Duration.ofHours(hours), 5, 10, LocalDate.of(2026, 1, 1));
    }

    static long[] hourly(int hour, long seconds) {
        long[] a = new long[24];
        a[hour] = seconds;
        return a;
    }

    static long[] evenly() {
        long[] a = new long[24];
        for (int i = 0; i < 24; i++) {
            a[i] = 3600;
        }
        return a;
    }

    static YearStats base(Duration total, List<ServerStat> servers, long[] hours) {
        return new YearStats(2026, total, servers.size(), servers,
                servers.isEmpty() ? Optional.empty() : Optional.of(servers.get(0)), Optional.empty(), hours, 0,
                Optional.empty(), 0, Optional.empty(), 0, 0, Optional.empty(), false, Duration.ZERO);
    }

    static YearStats full() {
        List<ServerStat> servers = List.of(server("a.net", 100), server("b.net", 50), server("c.net", 30),
                server("d.net", 20), server("e.net", 10), server("f.net", 4));
        Session s = new Session("me", Session.Target.server("a.net"), LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 14, 0));
        return new YearStats(2026, Duration.ofHours(214), 6, servers, Optional.of(servers.get(0)), Optional.of(s),
                evenly(), 9, Optional.of(LocalDate.of(2026, 1, 2)), 300, Optional.of("lol"), 4, 2,
                Optional.of("Steve"), true, Duration.ofHours(150));
    }

    static WorldStats world(long mined, long placed, long deaths, long mobKills) {
        return new WorldStats(mined, placed, 400, 200, mobKills, deaths, Optional.of("Zombie"));
    }

    static YearStats with(YearStats y, Duration total, int messages, int streak, int serverDeaths) {
        return new YearStats(y.year(), total, y.uniqueServers(), y.serversByTime(), y.mostActive(),
                y.longestSession(), y.secondsByHour(), streak, y.firstDay(), messages, y.topWord(), serverDeaths,
                y.serverKills(), y.topServerKiller(), y.hasPreviousYear(), y.previousYearTime());
    }
}
