package com.wrapped.chat;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OwnMessageMatcher {
    private static final List<String> DIVIDERS = List.of(
            " " + (char) 0xBB + " ", " " + (char) 0x2022 + " ", " | ",
            " " + (char) 0x2192 + " ", " " + (char) 0x279C + " ", " " + (char) 0x27A1 + " ", " " + (char) 0x25B8 + " ");
    private static final Pattern SENDER = Pattern.compile(
            "(?:^|(?<=[ \\]<>]))([^ \\]<>]+)(: |> |"
                    + String.join("|", DIVIDERS.stream().map(Pattern::quote).toList()) + ")");

    private OwnMessageMatcher() {
    }

    public record Sender(String name, int end) {
    }

    public static Optional<Sender> senderMatch(String chatText) {
        if (chatText == null) {
            return Optional.empty();
        }
        Matcher m = SENDER.matcher(chatText);
        int from = 0;
        while (from <= chatText.length() && m.find(from)) {
            if (DIVIDERS.contains(m.group(2)) && SENDER.matcher(chatText).region(m.end(), chatText.length()).find()) {
                from = m.end();
                continue;
            }
            return Optional.of(new Sender(m.group(1), m.end()));
        }
        return Optional.empty();
    }

    public static Optional<String> sender(String chatText) {
        return senderMatch(chatText).map(Sender::name);
    }

    public static Optional<String> ownMessage(String chatText, String name) {
        if (chatText == null || name == null || name.isEmpty() || !chatText.contains(name)) {
            return Optional.empty();
        }
        Optional<Sender> sender = senderMatch(chatText);
        if (sender.isEmpty() || !sender.get().name().equals(name)) {
            return Optional.empty();
        }
        String body = chatText.substring(sender.get().end()).strip();
        return body.isEmpty() || body.startsWith("/") ? Optional.empty() : Optional.of(body);
    }
}
