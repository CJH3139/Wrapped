package com.wrapped.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class TextFit {
    static final String ELLIPSIS = "...";

    private TextFit() {
    }

    public static String ellipsize(String text, int maxWidth, ToIntFunction<String> width) {
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width.applyAsInt(text.substring(0, mid) + ELLIPSIS) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        String fitted = text.substring(0, lo).stripTrailing() + ELLIPSIS;
        return width.applyAsInt(fitted) <= maxWidth ? fitted : "";
    }

    public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String joined = line.length() == 0 ? word : line + " " + word;
            if (width.applyAsInt(joined) <= maxWidth) {
                line.setLength(0);
                line.append(joined);
                continue;
            }
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            String rest = word;
            while (width.applyAsInt(rest) > maxWidth && rest.length() > 1) {
                int cut = 1;
                while (cut < rest.length() && width.applyAsInt(rest.substring(0, cut + 1)) <= maxWidth) {
                    cut++;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
