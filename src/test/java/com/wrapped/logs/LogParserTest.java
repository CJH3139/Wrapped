package com.wrapped.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogParserTest {
    private static List<LogEvent> one(String line) {
        return LogParser.parse(List.of(line));
    }

    @Test
    void userSet() {
        assertEquals(List.of(new LogEvent.UserSet(LocalTime.of(23, 27, 16), "iCJH")),
                one("[23:27:16] [Render thread/INFO]: Setting user: iCJH"));
    }

    @Test
    void serverConnect() {
        assertEquals(List.of(new LogEvent.ServerConnect(LocalTime.of(23, 27, 54), "minehut.com", 25565)),
                one("[23:27:54] [Render thread/INFO]: Connecting to minehut.com, 25565"));
    }

    @Test
    void voiceServerIgnored() {
        assertTrue(one("[23:27:36] [feather-scheduler-0/INFO]: Connecting to voice server...").isEmpty());
    }

    @Test
    void worldJoin() {
        assertEquals(List.of(new LogEvent.WorldJoin(LocalTime.of(0, 7, 51), "iCJH")),
                one("[00:07:51] [Server thread/INFO]: iCJH[local:E:02b6151a] logged in with entity id 11302 at (-0.5, -60.0, 8.5)"));
    }

    @Test
    void leaveVariants() {
        LocalTime t = LocalTime.of(12, 0, 0);
        assertEquals(List.of(new LogEvent.Leave(t)), one("[12:00:00] [Render thread/INFO]: Client disconnected with reason: Disconnected"));
        assertEquals(List.of(new LogEvent.Leave(t)), one("[12:00:00] [Server thread/INFO]: Stopping singleplayer server as player logged out"));
        assertEquals(List.of(new LogEvent.Leave(t)), one("[12:00:00] [Server thread/INFO]: iCJH lost connection: Disconnected"));
        assertTrue(one("[12:00:00] [Netty Epoll Server IO #1/INFO]: iCJH lost connection: Disconnected").isEmpty());
    }

    @Test
    void gameStop() {
        assertEquals(List.of(new LogEvent.GameStop(LocalTime.of(13, 44, 25))),
                one("[13:44:25] [Render thread/INFO]: Stopping!"));
    }

    @Test
    void chat() {
        assertEquals(List.of(new LogEvent.Chat(LocalTime.of(23, 50, 35), "[25\u2042] MINER iCJH: me")),
                one("[23:50:35] [Render thread/INFO]: [System] [CHAT] [25\u2042] MINER iCJH: me"));
    }

    @Test
    void couldNotConnectIsLeave() {
        assertEquals(List.of(new LogEvent.Leave(LocalTime.of(18, 45, 4))),
                one("[18:45:04] [Server Connector #9/ERROR]: Couldn't connect to server"));
        assertTrue(one("[18:45:04] [Render thread/INFO]: Couldn't connect to server").isEmpty());
    }

    @Test
    void brokenPrefixIgnored() {
        assertTrue(one("[00:00:05] [Render thread/WARN[00:00:05] [Render thread/INFO]: [System] [CHAT] hi").isEmpty());
        assertTrue(one("[0a:00:05] [Render thread/INFO]: Stopping!").isEmpty());
        assertTrue(one("[00:00:05] [Render thread/INFO]:").isEmpty());
    }

    @Test
    void chatMentioningJoinOrConnectStaysChat() {
        assertEquals(List.of(new LogEvent.Chat(LocalTime.of(1, 2, 3), "<Bob> Connecting to x, 1")),
                one("[01:02:03] [Render thread/INFO]: [CHAT] <Bob> Connecting to x, 1"));
        assertEquals(List.of(new LogEvent.Chat(LocalTime.of(1, 2, 3), "<Bob> logged in with entity id")),
                one("[01:02:03] [Render thread/INFO]: [CHAT] <Bob> logged in with entity id"));
    }

    @Test
    void streamingTracksFirstAndLastTime() {
        LogParser.Lines lines = new LogParser.Lines();
        assertTrue(lines.firstTime().isEmpty());
        lines.accept("noise");
        lines.accept("[23:59:58] [Render thread/INFO]: a");
        lines.accept("[00:00:01] [Render thread/INFO]: b");
        lines.accept("tail");
        assertEquals(LocalTime.of(23, 59, 58), lines.firstTime().orElseThrow());
        assertEquals(LocalTime.of(0, 0, 1), lines.lastTime().orElseThrow());
    }

    @Test
    void nonMatchingIgnored() {
        assertTrue(one("garbage line").isEmpty());
    }

    @Test
    void lastTime() {
        assertEquals(LocalTime.of(0, 7, 51), LogParser.lastTime(List.of(
                "[23:27:16] [Render thread/INFO]: x", "[00:07:51] [Server thread/INFO]: y", "tail")).orElseThrow());
        assertTrue(LogParser.lastTime(List.of("nothing")).isEmpty());
    }
}
