package com.wrapped.chat;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WordCounter {
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "you", "for", "that", "this", "with", "are", "was", "have", "not", "but", "what", "all",
            "can", "get", "just", "like", "its", "it's", "im", "dont", "don", "didn", "doesn", "isn", "wasn", "cant",
            "your", "you're", "they", "them", "then", "than", "there", "their", "from", "were", "has", "had", "will",
            "would", "could", "should", "one", "out", "now", "too", "got", "know", "want", "see", "also", "about",
            "here", "well", "how", "why", "who", "when", "where", "yes", "yeah", "yea", "okay", "gonna", "wanna",
            "any", "some", "been", "being", "did", "does", "doing", "our", "his", "her", "hers", "him", "she",
            "these", "those", "into", "onto", "over", "under", "again", "more", "most", "very", "much", "many",
            "only", "own", "same", "such", "off", "each", "both", "few", "other", "which", "while", "because",
            "until", "after", "before", "through", "above", "below", "yours", "mine", "myself", "yourself", "theirs",
            "let", "lets", "didnt", "doesnt", "isnt", "wasnt", "wont", "ill", "ive", "youre", "thats", "theres",
            "whats", "hes", "shes", "aren", "won", "couldn", "shouldn", "wouldn", "haven", "hasn", "may", "might",
            "must", "yet", "nor", "every", "ever", "even", "still");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+(?:'[\\p{L}]+)*");

    private final Map<String, Integer> counts = new HashMap<>();

    public static boolean isCountable(String word) {
        return word.length() >= 3 && word.indexOf('\'') < 0 && !STOPWORDS.contains(word)
                && word.chars().noneMatch(Character::isDigit);
    }

    public void add(String message) {
        add(message, null);
    }

    public void add(String message, String skip) {
        if (message == null) {
            return;
        }
        String skipped = skip == null ? null : skip.toLowerCase(Locale.ROOT);
        Matcher m = TOKEN.matcher(message);
        while (m.find()) {
            String word = m.group().toLowerCase(Locale.ROOT);
            if (isCountable(word) && !word.equals(skipped)) {
                counts.merge(word, 1, Integer::sum);
            }
        }
    }

    public Optional<String> top() {
        return counts.entrySet().stream()
                .min(Comparator.<Map.Entry<String, Integer>>comparingInt(e -> -e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey);
    }

    public Map<String, Integer> counts() {
        return Collections.unmodifiableMap(counts);
    }
}
