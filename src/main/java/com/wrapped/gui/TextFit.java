package com.wrapped.gui;

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
}
