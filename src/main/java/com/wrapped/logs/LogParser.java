package com.wrapped.logs;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogParser {
    private static final Pattern CONNECT = Pattern.compile("^Connecting to ([^,\\s]+), (\\d+)\\s*$");
    private static final Pattern JOIN = Pattern.compile("^(\\S+)\\[local:[^\\]]*\\] logged in with entity id");
    private static final String CONNECTING = "Connecting to ";
    private static final String JOINED = "logged in with entity id";
    private static final String SETTING_USER = "Setting user: ";
    private static final String CHAT = "[CHAT] ";
    private static final String LOST = " lost connection:";

    private LogParser() {
    }

    public static final class Lines {
        private int lastSecond = -1;
        private int firstSecond = -1;

        public LogEvent accept(String line) {
            int seconds = prefixSeconds(line);
            if (seconds < 0) {
                return null;
            }
            if (firstSecond < 0) {
                firstSecond = seconds;
            }
            lastSecond = seconds;
            int close = line.indexOf(']', 12);
            String thread = line.substring(12, close);
            String msg = line.substring(close + 3);
            return event(LocalTime.ofSecondOfDay(seconds), thread, msg);
        }

        public Optional<LocalTime> firstTime() {
            return firstSecond < 0 ? Optional.empty() : Optional.of(LocalTime.ofSecondOfDay(firstSecond));
        }

        public Optional<LocalTime> lastTime() {
            return lastSecond < 0 ? Optional.empty() : Optional.of(LocalTime.ofSecondOfDay(lastSecond));
        }
    }

    public static List<LogEvent> parse(List<String> lines) {
        Lines parser = new Lines();
        List<LogEvent> events = new ArrayList<>();
        for (String line : lines) {
            LogEvent event = parser.accept(line);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    public static Optional<LocalTime> lastTime(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            int seconds = prefixSeconds(lines.get(i));
            if (seconds >= 0) {
                return Optional.of(LocalTime.ofSecondOfDay(seconds));
            }
        }
        return Optional.empty();
    }

    static int prefixSeconds(String line) {
        if (line.length() < 16 || line.charAt(0) != '[' || line.charAt(3) != ':' || line.charAt(6) != ':'
                || line.charAt(9) != ']' || line.charAt(10) != ' ' || line.charAt(11) != '[') {
            return -1;
        }
        int h = twoDigits(line, 1);
        int m = twoDigits(line, 4);
        int s = twoDigits(line, 7);
        if (h < 0 || m < 0 || s < 0) {
            return -1;
        }
        int close = line.indexOf(']', 12);
        if (close <= 12 || close + 2 >= line.length() || line.charAt(close + 1) != ':' || line.charAt(close + 2) != ' ') {
            return -1;
        }
        return Math.min(h, 23) * 3600 + Math.min(m, 59) * 60 + Math.min(s, 59);
    }

    private static int twoDigits(String line, int at) {
        char a = line.charAt(at);
        char b = line.charAt(at + 1);
        if (a < '0' || a > '9' || b < '0' || b > '9') {
            return -1;
        }
        return (a - '0') * 10 + (b - '0');
    }

    private static LogEvent event(LocalTime time, String thread, String msg) {
        if (msg.startsWith(SETTING_USER)) {
            return new LogEvent.UserSet(time, msg.substring(SETTING_USER.length()).trim());
        }
        if (msg.startsWith(CONNECTING)) {
            Matcher connect = CONNECT.matcher(msg);
            if (connect.matches()) {
                try {
                    return new LogEvent.ServerConnect(time, connect.group(1), Integer.parseInt(connect.group(2)));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        if (msg.contains(JOINED)) {
            Matcher join = JOIN.matcher(msg);
            if (join.find()) {
                return new LogEvent.WorldJoin(time, join.group(1));
            }
        }
        if (msg.startsWith("Client disconnected with reason:") || msg.startsWith("Stopping singleplayer server")) {
            return new LogEvent.Leave(time);
        }
        if (thread.startsWith("Server thread") && lostConnection(msg)) {
            return new LogEvent.Leave(time);
        }
        if (thread.startsWith("Server Connector") && msg.startsWith("Couldn't connect to server")) {
            return new LogEvent.Leave(time);
        }
        int chat = msg.indexOf(CHAT);
        if (chat >= 0) {
            return new LogEvent.Chat(time, msg.substring(chat + CHAT.length()));
        }
        if (msg.startsWith("Stopping!")) {
            return new LogEvent.GameStop(time);
        }
        return null;
    }

    private static boolean lostConnection(String msg) {
        int at = msg.indexOf(LOST);
        if (at <= 0) {
            return false;
        }
        for (int i = 0; i < at; i++) {
            if (Character.isWhitespace(msg.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
