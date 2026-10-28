package com.wrapped.stats;

import com.wrapped.sessions.Session;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FileRecord(String fileName, boolean latest, String fingerprint, LocalDate date, List<Session> sessions,
        Map<String, Map<Integer, ChatSummary>> chat, Edges edges) {
    public static final String UNKNOWN = "";

    public record Edges(boolean leadingUnknown, LocalDateTime leadStart, LocalDateTime leadEnd, boolean leadOnly,
            String endAccount, Session.Target openAtEnd, String leadAccount) {
        public static final Edges NONE = new Edges(false, null, null, false, UNKNOWN, null, UNKNOWN);

        public Edges {
            endAccount = endAccount == null ? UNKNOWN : endAccount;
            leadAccount = leadAccount == null ? UNKNOWN : leadAccount;
        }
    }

    public FileRecord {
        sessions = List.copyOf(sessions);
        Map<String, Map<Integer, ChatSummary>> copy = new HashMap<>();
        chat.forEach((account, years) -> copy.put(account, Map.copyOf(years)));
        chat = Map.copyOf(copy);
        edges = edges == null ? Edges.NONE : edges;
    }

    public FileRecord(String fileName, boolean latest, String fingerprint, LocalDate date, List<Session> sessions,
            Map<String, Map<Integer, ChatSummary>> chat) {
        this(fileName, latest, fingerprint, date, sessions, chat, Edges.NONE);
    }
}
