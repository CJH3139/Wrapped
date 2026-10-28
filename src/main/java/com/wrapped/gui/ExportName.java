package com.wrapped.gui;

import com.wrapped.cards.Card;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ExportName {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private ExportName() {
    }

    static String cardName(Card card) {
        return card.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    static String fileName(int year, String cardName, LocalDateTime time) {
        return base(year, cardName, time) + ".png";
    }

    private static String base(int year, String cardName, LocalDateTime time) {
        return year + "-" + cardName + "-" + STAMP.format(time);
    }

    static Path directory(Path gameDir) {
        return gameDir.resolve("screenshots").resolve("wrapped");
    }

    static Path resolve(Path dir, int year, String cardName, LocalDateTime time) {
        String base = base(year, cardName, time);
        Path file = dir.resolve(base + ".png");
        for (int i = 1; Files.exists(file); i++) {
            file = dir.resolve(base + "_" + i + ".png");
        }
        return file;
    }
}
