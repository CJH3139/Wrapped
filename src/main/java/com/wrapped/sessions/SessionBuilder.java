package com.wrapped.sessions;

import com.wrapped.logs.LogEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SessionBuilder {
    static final Duration RETRY_WINDOW = Duration.ofSeconds(60);

    private final List<Session> out = new ArrayList<>();
    private LocalDate date;
    private LocalTime prev;
    private LocalDateTime now;
    private String account = "";
    private Session.Target openTarget;
    private LocalDateTime openStart;
    private String openAccount = "";
    private Session.Target retryTarget;
    private LocalDateTime retryAt;

    public SessionBuilder(LocalDate fileDate) {
        this.date = fileDate;
    }

    public static List<Session> build(LocalDate fileDate, List<LogEvent> events, Optional<LocalTime> lastTime) {
        SessionBuilder builder = new SessionBuilder(fileDate);
        for (LogEvent event : events) {
            builder.accept(event);
        }
        return builder.finish(lastTime);
    }

    public LocalDateTime now() {
        return now;
    }

    public Optional<Session.Target> open() {
        return Optional.ofNullable(openTarget);
    }

    public void accept(LogEvent event) {
        LocalTime t = event.time();
        if (prev != null && t.isBefore(prev)) {
            date = date.plusDays(1);
        }
        prev = t;
        now = date.atTime(t);
        if (event instanceof LogEvent.Chat chat) {
        } else if (event instanceof LogEvent.Leave leave) {
            close(now);
        } else if (event instanceof LogEvent.GameStop stop) {
            close(now);
            retryTarget = null;
        } else if (event instanceof LogEvent.UserSet set) {
            close(now);
            retryTarget = null;
            account = set.name();
        } else if (event instanceof LogEvent.ServerConnect connect) {
            connect(Session.Target.server(HostNormalizer.normalize(connect.host())));
        } else if (event instanceof LogEvent.WorldJoin join) {
            close(now);
            retryTarget = null;
            openTarget = Session.Target.world();
            openStart = now;
            openAccount = account;
        }
    }

    private void connect(Session.Target target) {
        boolean retry = target.equals(retryTarget) && retryAt != null
                && Duration.between(retryAt, now).compareTo(RETRY_WINDOW) <= 0;
        retryAt = now;
        if (retry && target.equals(openTarget)) {
            return;
        }
        if (retry && openTarget == null && openStart != null) {
            if (!out.isEmpty()) {
                Session last = out.get(out.size() - 1);
                if (last.target().equals(target) && last.start().equals(openStart)) {
                    out.remove(out.size() - 1);
                }
            }
            openTarget = target;
            openAccount = account;
            return;
        }
        close(now);
        retryTarget = target;
        openTarget = target;
        openStart = now;
        openAccount = account;
    }

    private void close(LocalDateTime end) {
        add(openAccount, openTarget, openStart, end);
        openTarget = null;
    }

    public LocalDateTime endAt(Optional<LocalTime> lastTime) {
        if (lastTime.isPresent()) {
            LocalDate endDate = date;
            if (prev != null && lastTime.get().isBefore(prev)) {
                endDate = date.plusDays(1);
            }
            return endDate.atTime(lastTime.get());
        }
        return prev == null ? null : date.atTime(prev);
    }

    public List<Session> finish(Optional<LocalTime> lastTime) {
        if (openTarget != null) {
            add(openAccount, openTarget, openStart, endAt(lastTime));
        }
        return out;
    }

    private void add(String sessionAccount, Session.Target target, LocalDateTime start, LocalDateTime end) {
        if (target == null) {
            return;
        }
        Session s = new Session(sessionAccount, target, start, end);
        if (!s.duration().isNegative() && !s.duration().isZero()) {
            out.add(s);
        }
    }
}
