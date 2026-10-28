package com.wrapped.sessions;

import java.time.Duration;
import java.time.LocalDateTime;

public record Session(String account, Target target, LocalDateTime start, LocalDateTime end, boolean continued) {
    public enum Kind {
        SERVER,
        WORLD
    }

    public record Target(Kind kind, String id) {
        public static final String SINGLEPLAYER = "singleplayer";

        public static Target server(String normalizedHost) {
            return new Target(Kind.SERVER, normalizedHost);
        }

        public static Target world() {
            return new Target(Kind.WORLD, SINGLEPLAYER);
        }
    }

    public Session(String account, Target target, LocalDateTime start, LocalDateTime end) {
        this(account, target, start, end, false);
    }

    public Session withAccount(String newAccount) {
        return new Session(newAccount, target, start, end, continued);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
