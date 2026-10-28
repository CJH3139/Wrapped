package com.wrapped.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LogFileNameTest {
    @Test
    void parsesRotatedLog() {
        assertEquals(new LogFileName(LocalDate.of(2026, 3, 18), 3, false), LogFileName.parse("2026-03-18-3.log.gz").orElseThrow());
    }

    @Test
    void parsesUncompressedRotatedLog() {
        assertEquals(new LogFileName(LocalDate.of(2025, 9, 25), 3, false), LogFileName.parse("2025-09-25-3.log").orElseThrow());
    }

    @Test
    void parsesLatest() {
        assertTrue(LogFileName.parse("latest.log", LocalDate.of(2026, 10, 27)).orElseThrow().latest());
    }

    @Test
    void rejectsOthers() {
        assertTrue(LogFileName.parse("debug-1.log.gz").isEmpty());
        assertTrue(LogFileName.parse("telemetry").isEmpty());
    }
}
