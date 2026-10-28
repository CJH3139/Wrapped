package com.wrapped.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {
    @TempDir
    Path dir;

    @Test
    void defaultsWhenMissing() {
        assertFalse(Settings.load(dir).hideTopWord());
    }

    @Test
    void savesAndLoadsHideTopWord() {
        assertTrue(new Settings(true, false).save(dir));
        assertTrue(Files.exists(dir.resolve("wrapped").resolve("settings.json")));
        assertTrue(Settings.load(dir).hideTopWord());
        assertTrue(new Settings(false, false).save(dir));
        assertFalse(Settings.load(dir).hideTopWord());
    }

    @Test
    void writesHideTopWordKey() throws IOException {
        new Settings(true, false).save(dir);
        String json = Files.readString(dir.resolve("wrapped").resolve("settings.json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"hideTopWord\":true"));
    }

    @Test
    void corruptFileFallsBackToDefaults() throws IOException {
        Path file = dir.resolve("wrapped").resolve("settings.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{not json", StandardCharsets.UTF_8);
        assertEquals(new Settings(false, false), Settings.load(dir));
    }

    @Test
    void oldFileWithoutReduceMotionLoads() throws IOException {
        Path file = dir.resolve("wrapped").resolve("settings.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"hideTopWord\":true}", StandardCharsets.UTF_8);
        assertEquals(new Settings(true, false), Settings.load(dir));
    }

    @Test
    void roundTrips() {
        for (Settings s : new Settings[] {new Settings(false, false), new Settings(true, false),
                new Settings(false, true), new Settings(true, true)}) {
            assertTrue(s.save(dir));
            assertEquals(s, Settings.load(dir));
        }
    }

    @Test
    void defaultsToBothOff() {
        assertEquals(new Settings(false, false), Settings.load(dir));
    }
}
