package com.wrapped.stats;

import com.wrapped.chat.DeathMessages;
import com.wrapped.chat.OwnMessageMatcher;
import com.wrapped.chat.WordCounter;
import com.wrapped.logs.LogEvent;
import com.wrapped.logs.LogParser;
import com.wrapped.sessions.Session;
import com.wrapped.sessions.SessionBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FileRecords {
    private static final int WORD_LIMIT = 200;

    private FileRecords() {
    }

    private static final class Acc {
        int messages;
        final WordCounter words = new WordCounter();
        int deaths;
        int kills;
        final Map<String, Integer> killers = new HashMap<>();
    }

    private static boolean isSystemLine(String text, String account) {
        Optional<OwnMessageMatcher.Sender> sender = OwnMessageMatcher.senderMatch(text);
        return sender.isEmpty() || sender.get().end() > text.indexOf(account);
    }

    public static final class Builder {
        private final LocalDate date;
        private final String leadAccount;
        private final LogParser.Lines lines = new LogParser.Lines();
        private final SessionBuilder sessions;
        private final Map<String, Map<Integer, Acc>> accs = new HashMap<>();
        private String account;
        private String endAccount = FileRecord.UNKNOWN;
        private boolean boundarySeen;
        private boolean leadingUnknown;
        private LocalDateTime leadEnd;

        public Builder(LocalDate date, String leadAccount) {
            this.date = date;
            this.leadAccount = leadAccount == null ? FileRecord.UNKNOWN : leadAccount;
            this.sessions = new SessionBuilder(date);
        }

        public void accept(String line) {
            LogEvent event = lines.accept(line);
            if (event == null) {
                return;
            }
            sessions.accept(event);
            if (!boundarySeen && !(event instanceof LogEvent.Chat)) {
                boundarySeen = true;
                leadingUnknown = !(event instanceof LogEvent.UserSet);
                leadEnd = sessions.now();
            }
            if (event instanceof LogEvent.UserSet set) {
                account = set.name();
                endAccount = set.name();
            } else if (event instanceof LogEvent.Chat chat) {
                chat(chat.text());
            }
        }

        private void chat(String text) {
            String name = account != null ? account : leadAccount;
            if (name.isEmpty() || !text.contains(name)) {
                return;
            }
            String key = account != null ? account : FileRecord.UNKNOWN;
            Acc acc = accs.computeIfAbsent(key, k -> new HashMap<>())
                    .computeIfAbsent(sessions.now().getYear(), k -> new Acc());
            Optional<String> own = OwnMessageMatcher.ownMessage(text, name);
            if (own.isPresent()) {
                acc.messages++;
                acc.words.add(own.get(), name);
            } else if (isSystemLine(text, name)) {
                Optional<DeathMessages.Death> death = DeathMessages.parse(text, name);
                if (death.isPresent()) {
                    if (death.get().playerDied()) {
                        acc.deaths++;
                        if (!death.get().other().isEmpty()) {
                            acc.killers.merge(death.get().other(), 1, Integer::sum);
                        }
                    } else {
                        acc.kills++;
                    }
                }
            }
        }

        public FileRecord finish(String fileName, boolean latest, String fingerprint) {
            Optional<Session.Target> open = sessions.open();
            List<Session> built = sessions.finish(lines.lastTime());
            boolean unknown = boundarySeen ? leadingUnknown : lines.firstTime().isPresent();
            boolean leadOnly = !boundarySeen && unknown;
            LocalDateTime leadStart = null;
            LocalDateTime end = null;
            if (unknown) {
                leadStart = date.atTime(lines.firstTime().orElseThrow());
                end = boundarySeen ? leadEnd : sessions.endAt(lines.lastTime());
                if (end == null) {
                    end = leadStart;
                }
            }
            FileRecord.Edges edges = new FileRecord.Edges(unknown, leadStart, end, leadOnly, endAccount,
                    open.orElse(null), unknown ? leadAccount : FileRecord.UNKNOWN);
            return new FileRecord(fileName, latest, fingerprint, date, new ArrayList<>(built), summaries(), edges);
        }

        private Map<String, Map<Integer, ChatSummary>> summaries() {
            Map<String, Map<Integer, ChatSummary>> chat = new HashMap<>();
            for (Map.Entry<String, Map<Integer, Acc>> byAccount : accs.entrySet()) {
                Map<Integer, ChatSummary> years = new HashMap<>();
                for (Map.Entry<Integer, Acc> byYear : byAccount.getValue().entrySet()) {
                    Acc a = byYear.getValue();
                    years.put(byYear.getKey(), new ChatSummary(a.messages, limit(a.words.counts()), a.deaths, a.kills,
                            new HashMap<>(a.killers)));
                }
                chat.put(byAccount.getKey(), years);
            }
            return chat;
        }
    }

    private static Map<String, Integer> limit(Map<String, Integer> counts) {
        Map<String, Integer> words = new HashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(WORD_LIMIT)
                .forEach(e -> words.put(e.getKey(), e.getValue()));
        return words;
    }

    public static FileRecord parse(String fileName, boolean latest, String fingerprint, LocalDate date, List<String> lines) {
        return parse(fileName, latest, fingerprint, date, lines, FileRecord.UNKNOWN);
    }

    public static FileRecord parse(String fileName, boolean latest, String fingerprint, LocalDate date, List<String> lines,
            String leadAccount) {
        Builder builder = new Builder(date, leadAccount);
        for (String line : lines) {
            builder.accept(line);
        }
        return builder.finish(fileName, latest, fingerprint);
    }

    public static FileRecord stitch(FileRecord previous, FileRecord raw) {
        FileRecord.Edges edges = raw.edges();
        if (previous == null || !edges.leadingUnknown() || previous.date() == null || raw.date() == null
                || raw.date().isBefore(previous.date()) || raw.date().isAfter(previous.date().plusDays(1))) {
            return raw;
        }
        FileRecord.Edges before = previous.edges();
        String account = before.endAccount();
        if (account.isEmpty()) {
            return raw;
        }
        List<Session> sessions = new ArrayList<>();
        if (before.openAtEnd() != null && edges.leadStart() != null && edges.leadEnd() != null
                && edges.leadEnd().isAfter(edges.leadStart())) {
            sessions.add(new Session(account, before.openAtEnd(), edges.leadStart(), edges.leadEnd(), true));
        }
        for (Session s : raw.sessions()) {
            sessions.add(s.account().isEmpty() ? s.withAccount(account) : s);
        }
        Map<String, Map<Integer, ChatSummary>> chat = new HashMap<>();
        raw.chat().forEach((k, v) -> {
            if (!k.isEmpty()) {
                chat.put(k, new HashMap<>(v));
            }
        });
        Map<Integer, ChatSummary> lead = raw.chat().get(FileRecord.UNKNOWN);
        if (lead != null && edges.leadAccount().equals(account)) {
            Map<Integer, ChatSummary> target = chat.computeIfAbsent(account, k -> new HashMap<>());
            lead.forEach((year, summary) -> target.merge(year, summary, ChatSummary::plus));
        }
        String endAccount = edges.endAccount().isEmpty() ? account : edges.endAccount();
        Session.Target openAtEnd = edges.leadOnly() ? before.openAtEnd() : edges.openAtEnd();
        FileRecord.Edges stitched = new FileRecord.Edges(true, edges.leadStart(), edges.leadEnd(), edges.leadOnly(),
                endAccount, openAtEnd, edges.leadAccount());
        return new FileRecord(raw.fileName(), raw.latest(), raw.fingerprint(), raw.date(), sessions, chat, stitched);
    }

    public static List<FileRecord> stitchAll(List<FileRecord> raw) {
        List<FileRecord> out = new ArrayList<>(raw.size());
        FileRecord previous = null;
        for (FileRecord r : raw) {
            FileRecord s = stitch(previous, r);
            out.add(s);
            previous = s;
        }
        return out;
    }
}
