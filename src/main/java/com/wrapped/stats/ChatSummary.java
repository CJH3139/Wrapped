package com.wrapped.stats;

import java.util.HashMap;
import java.util.Map;

public record ChatSummary(int messagesSent, Map<String, Integer> wordCounts, int deaths, int kills, Map<String, Integer> killers) {
    public ChatSummary {
        wordCounts = Map.copyOf(wordCounts);
        killers = Map.copyOf(killers);
    }

    public ChatSummary plus(ChatSummary other) {
        Map<String, Integer> words = new HashMap<>(wordCounts);
        other.wordCounts.forEach((k, v) -> words.merge(k, v, Integer::sum));
        Map<String, Integer> by = new HashMap<>(killers);
        other.killers.forEach((k, v) -> by.merge(k, v, Integer::sum));
        return new ChatSummary(messagesSent + other.messagesSent, words, deaths + other.deaths, kills + other.kills, by);
    }
}
