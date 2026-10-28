package com.wrapped.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.sessions.Session;
import com.wrapped.stats.ChatSummary;
import com.wrapped.stats.FileRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceCacheTest {
    @TempDir
    Path dir;

    private static FileRecord sample() {
        Session a = new Session("iCJH", Session.Target.server("minehut.com"), LocalDateTime.of(2026, 3, 1, 10, 0), LocalDateTime.of(2026, 3, 1, 12, 30));
        Session b = new Session("iCJH", Session.Target.world(), LocalDateTime.of(2026, 3, 2, 10, 0), LocalDateTime.of(2026, 3, 2, 11, 0));
        ChatSummary chat = new ChatSummary(3, Map.of("diamonds", 2), 1, 2, Map.of("Zombie", 1));
        return new FileRecord("2026-03-01-1.log.gz", false, "abc", LocalDate.of(2026, 3, 1), List.of(a, b),
                Map.of("iCJH", Map.of(2026, chat)));
    }

    @Test
    void fileRecordRoundTrips() throws IOException {
        Path f = dir.resolve("r.json");
        FileRecord r = sample();
        Json.write(f, r);
        assertEquals(Optional.of(r), Json.read(f, FileRecord.class));
    }

    @Test
    void cacheRoundTrips() {
        InstanceCache cache = InstanceCache.load(dir);
        FileRecord r = sample();
        cache.put(r.fileName(), 10, 20, r);
        cache.save();
        InstanceCache again = InstanceCache.load(dir);
        assertEquals(Optional.of(r), again.get(r.fileName(), 10, 20));
        assertTrue(again.get(r.fileName(), 11, 20).isEmpty());
        assertTrue(again.get(r.fileName(), 10, 21).isEmpty());
    }

    @Test
    void differentVersionStartsEmpty() throws IOException {
        InstanceCache cache = InstanceCache.load(dir);
        FileRecord r = sample();
        cache.put(r.fileName(), 10, 20, r);
        cache.save();
        Path file = dir.resolve("wrapped").resolve("cache.json");
        Files.writeString(file, Files.readString(file).replace("\"version\": 2", "\"version\": 3").replace("\"version\":2", "\"version\":3"));
        assertTrue(InstanceCache.load(dir).get(r.fileName(), 10, 20).isEmpty());
    }

    @Test
    void corruptFileStartsEmpty() throws IOException {
        Files.createDirectories(dir.resolve("wrapped"));
        Files.writeString(dir.resolve("wrapped").resolve("cache.json"), "{not json");
        assertTrue(InstanceCache.load(dir).get("x", 1, 1).isEmpty());
    }

    @Test
    void saveReturnsFalseWhenDirectoryBlocked() throws IOException {
        Files.writeString(dir.resolve("wrapped"), "file");
        InstanceCache cache = InstanceCache.load(dir);
        assertFalse(cache.save());
    }

    @Test
    void saveReturnsTrueOnSuccess() {
        assertTrue(InstanceCache.load(dir).save());
    }
}
