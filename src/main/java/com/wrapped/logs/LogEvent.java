package com.wrapped.logs;

import java.time.LocalTime;

public sealed interface LogEvent permits LogEvent.UserSet, LogEvent.ServerConnect, LogEvent.WorldJoin, LogEvent.Leave, LogEvent.Chat, LogEvent.GameStop {
    LocalTime time();

    record UserSet(LocalTime time, String name) implements LogEvent {
    }

    record ServerConnect(LocalTime time, String host, int port) implements LogEvent {
    }

    record WorldJoin(LocalTime time, String name) implements LogEvent {
    }

    record Leave(LocalTime time) implements LogEvent {
    }

    record Chat(LocalTime time, String text) implements LogEvent {
    }

    record GameStop(LocalTime time) implements LogEvent {
    }
}
