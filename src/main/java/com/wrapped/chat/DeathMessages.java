package com.wrapped.chat;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeathMessages {
    public record Death(boolean playerDied, String other) {
    }

    private record Patterns(Pattern forward, Pattern solo, Pattern reverse) {
    }

    private static final String VERBS = "(?:was slain by|was shot by|was blown up by)";
    private static final Pattern USING_SUFFIX = Pattern.compile("\\s+using\\s+\\[.*$");
    private static final ConcurrentHashMap<String, Patterns> CACHE = new ConcurrentHashMap<>();

    private DeathMessages() {
    }

    private static Patterns patternsFor(String name) {
        return CACHE.computeIfAbsent(name, key -> {
            String n = Pattern.quote(key);
            return new Patterns(
                    Pattern.compile("(?<![^ ])" + n + " " + VERBS + " (.+)$"),
                    Pattern.compile("(?<![^ ])" + n + " (?:died|fell from a high place)(?!\\S)"),
                    Pattern.compile("(?<![^ ])(\\S+) " + VERBS + " " + n + "(?!\\S)"));
        });
    }

    public static Optional<Death> parse(String chatText, String name) {
        if (chatText == null || name == null || name.isEmpty() || !chatText.contains(name)) {
            return Optional.empty();
        }
        Patterns p = patternsFor(name);
        Matcher forward = p.forward().matcher(chatText);
        if (forward.find()) {
            String other = USING_SUFFIX.matcher(forward.group(1)).replaceFirst("").strip();
            return Optional.of(new Death(true, other));
        }
        if (p.solo().matcher(chatText).find()) {
            return Optional.of(new Death(true, ""));
        }
        Matcher reverse = p.reverse().matcher(chatText);
        if (reverse.find()) {
            return Optional.of(new Death(false, reverse.group(1)));
        }
        return Optional.empty();
    }
}
