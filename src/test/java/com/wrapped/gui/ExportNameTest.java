package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wrapped.cards.Card;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportNameTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 27, 9, 5, 3);

    @Test
    void cardNameIsLowercaseRecordName() {
        assertEquals("title", ExportName.cardName(new Card.Title(2026)));
        assertEquals("summary", ExportName.cardName(
                new Card.Summary(10, java.util.Optional.empty(), 2, com.wrapped.cards.PlayerType.MINER)));
        assertEquals("streak", ExportName.cardName(new Card.Streak(4)));
        assertEquals("servercount", ExportName.cardName(new Card.ServerCount(3)));
    }

    @Test
    void fileNameHasYearCardAndTimestamp() {
        assertEquals("2026-summary-20261027-090503.png", ExportName.fileName(2026, "summary", AT));
    }

    @Test
    void pathIsUnderScreenshotsWrapped() {
        Path game = Path.of("game");
        assertEquals(game.resolve("screenshots").resolve("wrapped"), ExportName.directory(game));
    }

    @Test
    void resolveKeepsPlainNameWhenFree(@TempDir Path dir) {
        assertEquals(dir.resolve("2026-streak-20261027-090503.png"), ExportName.resolve(dir, 2026, "streak", AT));
    }

    @Test
    void resolveAddsSuffixWhenTaken(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("2026-streak-20261027-090503.png"));
        assertEquals(dir.resolve("2026-streak-20261027-090503_1.png"), ExportName.resolve(dir, 2026, "streak", AT));
        Files.createFile(dir.resolve("2026-streak-20261027-090503_1.png"));
        assertEquals(dir.resolve("2026-streak-20261027-090503_2.png"), ExportName.resolve(dir, 2026, "streak", AT));
    }
}
