package com.wrapped.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.logs.LogEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SessionBuilderTest {
    private static final LocalDate DATE = LocalDate.of(2026, 3, 18);

    private static LocalTime t(int h, int m) {
        return LocalTime.of(h, m);
    }

    @Test
    void simpleServerSession() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.UserSet(t(10, 0), "a"),
                new LogEvent.ServerConnect(t(10, 1), "Minehut.com", 25565),
                new LogEvent.Leave(t(11, 1))), Optional.empty());
        assertEquals(1, s.size());
        assertEquals(Duration.ofMinutes(60), s.get(0).duration());
        assertEquals(new Session.Target(Session.Kind.SERVER, "minehut.com"), s.get(0).target());
        assertEquals("a", s.get(0).account());
    }

    @Test
    void crossesMidnight() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(t(23, 50), "x.net", 25565),
                new LogEvent.Leave(t(0, 20))), Optional.empty());
        assertEquals(1, s.size());
        assertEquals(Duration.ofMinutes(30), s.get(0).duration());
        assertEquals(LocalDateTime.of(2026, 3, 19, 0, 20), s.get(0).end());
        assertEquals("", s.get(0).account());
    }

    @Test
    void truncatedLogEndsAtLastTime() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(t(20, 0), "x.net", 25565)), Optional.of(t(21, 30)));
        assertEquals(1, s.size());
        assertEquals(Duration.ofMinutes(90), s.get(0).duration());
    }

    @Test
    void lastTimeAcrossMidnight() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.WorldJoin(t(23, 30), "w")), Optional.of(t(0, 10)));
        assertEquals(Duration.ofMinutes(40), s.get(0).duration());
        assertEquals(Session.Target.world(), s.get(0).target());
    }

    @Test
    void transferClosesAndReopens() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(t(10, 0), "a.net", 25565),
                new LogEvent.ServerConnect(t(10, 30), "b.net", 25565),
                new LogEvent.GameStop(t(11, 0))), Optional.empty());
        assertEquals(2, s.size());
        assertEquals(Duration.ofMinutes(30), s.get(0).duration());
        assertEquals(Duration.ofMinutes(30), s.get(1).duration());
        assertEquals("b.net", s.get(1).target().id());
    }

    @Test
    void chatDoesNotCloseAndZeroDropped() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(t(10, 0), "a.net", 25565),
                new LogEvent.Chat(t(10, 10), "hi"),
                new LogEvent.Leave(t(10, 20)),
                new LogEvent.WorldJoin(t(10, 30), "w"),
                new LogEvent.Leave(t(10, 30))), Optional.empty());
        assertEquals(1, s.size());
        assertEquals(Duration.ofMinutes(20), s.get(0).duration());
        assertTrue(s.get(0).end().isAfter(s.get(0).start()));
    }

    private static LocalTime ts(int h, int m, int sec) {
        return LocalTime.of(h, m, sec);
    }

    @Test
    void failedRetriesWithinAMinuteCollapseIntoOneSession() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(ts(10, 0, 0), "a.net", 25565),
                new LogEvent.Leave(ts(10, 0, 0)),
                new LogEvent.ServerConnect(ts(10, 0, 30), "a.net", 25565),
                new LogEvent.Leave(ts(10, 0, 31)),
                new LogEvent.ServerConnect(ts(10, 1, 20), "A.net", 25565),
                new LogEvent.Leave(ts(11, 0, 0))), Optional.empty());
        assertEquals(1, s.size());
        assertEquals(LocalDateTime.of(2026, 3, 18, 10, 0), s.get(0).start());
        assertEquals(LocalDateTime.of(2026, 3, 18, 11, 0), s.get(0).end());
    }

    @Test
    void repeatedConnectWhileOpenKeepsOneSession() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(ts(10, 0, 0), "a.net", 25565),
                new LogEvent.ServerConnect(ts(10, 0, 20), "a.net", 25565),
                new LogEvent.Leave(ts(10, 30, 0))), Optional.empty());
        assertEquals(1, s.size());
        assertEquals(Duration.ofMinutes(30), s.get(0).duration());
    }

    @Test
    void reconnectAfterAMinuteOrToAnotherHostIsNewSession() {
        List<Session> s = SessionBuilder.build(DATE, List.of(
                new LogEvent.ServerConnect(ts(10, 0, 0), "a.net", 25565),
                new LogEvent.Leave(ts(10, 0, 50)),
                new LogEvent.ServerConnect(ts(10, 1, 1), "a.net", 25565),
                new LogEvent.Leave(ts(10, 1, 10)),
                new LogEvent.ServerConnect(ts(10, 1, 20), "b.net", 25565),
                new LogEvent.Leave(ts(10, 2, 0))), Optional.empty());
        assertEquals(3, s.size());
        assertEquals("b.net", s.get(2).target().id());
    }
}
