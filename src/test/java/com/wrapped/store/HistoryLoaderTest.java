package com.wrapped.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.stats.FileRecord;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryLoaderTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    @TempDir
    Path dir;

    Path logs;

    @BeforeEach
    void setUp() throws IOException {
        logs = Files.createDirectories(dir.resolve("logs"));
        gz("2026-01-01-1.log.gz", "aaaa");
        gz("2026-01-02-1.log.gz", "bbbb");
        Files.writeString(logs.resolve("latest.log"), "current\n", StandardCharsets.UTF_8);
    }

    private Path gz(String name, String content) throws IOException {
        Path f = logs.resolve(name);
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(f))) {
            out.write((content + "\n").getBytes(StandardCharsets.UTF_8));
        }
        return f;
    }

    private Map<String, FileRecord> load() {
        return HistoryLoader.load(dir, InstanceCache.load(dir), TODAY).stream()
                .collect(Collectors.toMap(FileRecord::fileName, Function.identity()));
    }

    @Test
    void secondLoadReusesCache() throws IOException {
        Map<String, FileRecord> first = load();
        assertEquals(3, first.size());
        Path f = logs.resolve("2026-01-01-1.log.gz");
        FileTime mtime = Files.getLastModifiedTime(f);
        long size = Files.size(f);
        gz("2026-01-01-1.log.gz", "zzzz");
        assertEquals(size, Files.size(f));
        Files.setLastModifiedTime(f, mtime);
        Map<String, FileRecord> second = load();
        assertEquals(first.get("2026-01-01-1.log.gz"), second.get("2026-01-01-1.log.gz"));
    }

    @Test
    void changedFileIsReparsed() throws IOException {
        Map<String, FileRecord> first = load();
        Path f = logs.resolve("2026-01-01-1.log.gz");
        gz("2026-01-01-1.log.gz", "a much longer replacement line");
        Files.setLastModifiedTime(f, FileTime.fromMillis(Files.getLastModifiedTime(f).toMillis() + 5000));
        Map<String, FileRecord> second = load();
        assertFalse(first.get("2026-01-01-1.log.gz").fingerprint().equals(second.get("2026-01-01-1.log.gz").fingerprint()));
        assertEquals(Fingerprint.of("a much longer replacement line\n".getBytes(StandardCharsets.UTF_8)), second.get("2026-01-01-1.log.gz").fingerprint());
    }

    @Test
    void latestLogNotCached() throws IOException {
        Map<String, FileRecord> records = load();
        assertTrue(records.get("latest.log").latest());
        String json = Files.readString(dir.resolve("wrapped").resolve("cache.json"));
        assertFalse(json.contains("latest.log"));
        assertTrue(json.contains("2026-01-01-1.log.gz"));
    }

    @Test
    void latestIsReparsedEveryTime() throws IOException {
        load();
        Files.writeString(logs.resolve("latest.log"), "different\n", StandardCharsets.UTF_8);
        assertEquals(Fingerprint.of("different\n".getBytes(StandardCharsets.UTF_8)), load().get("latest.log").fingerprint());
    }

    @Test
    void ignoresUnrelatedAndSkipsUnreadable() throws IOException {
        Files.writeString(logs.resolve("debug.log"), "x");
        Files.writeString(logs.resolve("2026-02-01-1.log.gz"), "not gzip");
        Map<String, FileRecord> records = load();
        assertEquals(3, records.size());
        assertFalse(records.containsKey("debug.log"));
        assertFalse(Files.readString(dir.resolve("wrapped").resolve("cache.json")).contains("2026-02-01-1.log.gz"));
    }

    @Test
    void removedFilesDroppedFromCache() throws IOException {
        load();
        Files.delete(logs.resolve("2026-01-02-1.log.gz"));
        load();
        assertFalse(Files.readString(dir.resolve("wrapped").resolve("cache.json")).contains("2026-01-02-1.log.gz"));
    }

    @Test
    void missingLogsDirIsEmpty() throws IOException {
        Path other = Files.createDirectory(dir.resolve("other"));
        assertTrue(HistoryLoader.load(other, InstanceCache.load(other), TODAY).isEmpty());
        assertFalse(Files.exists(other.resolve("wrapped").resolve("cache.json")));
    }

    @Test
    void unchangedLoadDoesNotRewriteCache() throws IOException {
        load();
        Path cacheFile = dir.resolve("wrapped").resolve("cache.json");
        FileTime old = FileTime.fromMillis(1_000_000_000L);
        Files.setLastModifiedTime(cacheFile, old);
        load();
        assertEquals(old, Files.getLastModifiedTime(cacheFile));
    }

    private void gzLines(String name, String... lines) throws IOException {
        gz(name, String.join("\n", lines));
    }

    private List<FileRecord> loadAs(String account) {
        return HistoryLoader.load(dir, InstanceCache.load(dir), TODAY, account);
    }

    private void clearLogs() throws IOException {
        try (java.util.stream.Stream<Path> files = Files.list(logs)) {
            for (Path f : files.toList()) {
                Files.delete(f);
            }
        }
    }

    @Test
    void midnightRolloverKeepsAccountServerAndChat() throws IOException {
        clearLogs();
        gzLines("2026-03-01-1.log.gz",
                "[22:00:00] [main/INFO]: Setting user: iCJH",
                "[22:00:05] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[23:00:00] [Render thread/INFO]: [CHAT] <iCJH> hello diamonds",
                "[23:59:59] [Render thread/INFO]: [CHAT] <Bob> late");
        gzLines("2026-03-02-1.log.gz",
                "[00:00:00] [Render thread/INFO]: [CHAT] <iCJH> diamonds again",
                "[00:30:00] [Render thread/INFO]: [CHAT] iCJH was slain by Zombie",
                "[01:00:00] [Render thread/INFO]: Client disconnected with reason: Disconnected",
                "[01:10:00] [Render thread/INFO]: Stopping!");
        List<FileRecord> records = loadAs("iCJH");
        assertEquals(List.of("2026-03-01-1.log.gz", "2026-03-02-1.log.gz"),
                records.stream().map(FileRecord::fileName).toList());
        com.wrapped.stats.YearStats y = com.wrapped.stats.YearStatsBuilder.build(2026, "iCJH", records, TODAY);
        assertEquals(java.time.Duration.ofSeconds(7194 + 3600), y.totalTime());
        assertEquals("minehut.com", y.serversByTime().get(0).host());
        assertEquals(1, y.serversByTime().get(0).joins());
        assertEquals(2, y.messagesSent());
        assertEquals(1, y.serverDeaths());
        assertEquals(java.util.Optional.of("diamonds"), y.topWord());
        FileRecord b = records.get(1);
        assertTrue(b.sessions().stream().allMatch(x -> x.account().equals("iCJH")));
        assertFalse(b.chat().containsKey(""));
        assertEquals(java.time.LocalDateTime.of(2026, 3, 2, 0, 0), b.sessions().get(0).start());
    }

    @Test
    void rolloverChainsThroughWholeDayContinuations() throws IOException {
        clearLogs();
        gzLines("2026-03-01-1.log.gz",
                "[23:00:00] [main/INFO]: Setting user: iCJH",
                "[23:00:00] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[23:59:59] [Render thread/INFO]: x");
        gzLines("2026-03-02-1.log.gz",
                "[00:00:00] [Render thread/INFO]: y",
                "[23:59:59] [Render thread/INFO]: z");
        gzLines("2026-03-03-1.log.gz",
                "[00:00:00] [Render thread/INFO]: [CHAT] <iCJH> hi there",
                "[01:00:00] [Render thread/INFO]: Stopping!");
        List<FileRecord> records = loadAs("iCJH");
        com.wrapped.stats.YearStats y = com.wrapped.stats.YearStatsBuilder.build(2026, "iCJH", records, TODAY);
        assertEquals(java.time.Duration.ofSeconds(3599 + 86399 + 3600), y.totalTime());
        assertEquals(1, y.messagesSent());
        assertEquals(1, y.serversByTime().get(0).joins());
    }

    @Test
    void freshLaunchAfterCrashIsNotStitched() throws IOException {
        clearLogs();
        gzLines("2026-03-01-1.log.gz",
                "[10:00:00] [main/INFO]: Setting user: iCJH",
                "[10:00:00] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[11:00:00] [Render thread/INFO]: x");
        gzLines("2026-03-01-2.log.gz",
                "[12:00:00] [main/INFO]: Loading",
                "[12:00:01] [main/INFO]: Setting user: iCJH",
                "[12:30:00] [Render thread/INFO]: Stopping!");
        com.wrapped.stats.YearStats y = com.wrapped.stats.YearStatsBuilder.build(2026, "iCJH", loadAs("iCJH"), TODAY);
        assertEquals(java.time.Duration.ofHours(1), y.totalTime());
    }

    @Test
    void sortsByDateThenNumericIndexWithLatestLast() throws IOException {
        clearLogs();
        gz("2026-01-01-10.log.gz", "x");
        gz("2026-01-01-2.log.gz", "x");
        gz("2025-12-31-3.log.gz", "x");
        gz("2026-10-27-1.log.gz", "x");
        Files.writeString(logs.resolve("latest.log"), "y\n", StandardCharsets.UTF_8);
        assertEquals(List.of("2025-12-31-3.log.gz", "2026-01-01-2.log.gz", "2026-01-01-10.log.gz",
                "2026-10-27-1.log.gz", "latest.log"), loadAs("iCJH").stream().map(FileRecord::fileName).toList());
    }

    @Test
    void latestLogIsDatedToday() throws IOException {
        Files.setAttribute(logs.resolve("latest.log"), "creationTime", FileTime.fromMillis(0));
        assertEquals(TODAY, load().get("latest.log").date());
    }

    @Test
    void cachedContinuationIsReparsedForAnotherAccount() throws IOException {
        clearLogs();
        gzLines("2026-03-01-1.log.gz",
                "[22:00:00] [main/INFO]: Setting user: Alt",
                "[22:00:05] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[23:59:59] [Render thread/INFO]: x");
        gzLines("2026-03-02-1.log.gz",
                "[00:00:00] [Render thread/INFO]: [CHAT] <Alt> hello friends",
                "[01:00:00] [Render thread/INFO]: Stopping!");
        com.wrapped.stats.YearStats mine = com.wrapped.stats.YearStatsBuilder.build(2026, "Alt", loadAs("iCJH"), TODAY);
        assertEquals(0, mine.messagesSent());
        com.wrapped.stats.YearStats alt = com.wrapped.stats.YearStatsBuilder.build(2026, "Alt", loadAs("Alt"), TODAY);
        assertEquals(1, alt.messagesSent());
    }

    @Test
    void manyFilesParseInParallelAndStayOrdered() throws IOException {
        clearLogs();
        List<String> expected = new java.util.ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            String name = "2026-02-01-" + i + ".log.gz";
            String start = String.format("[%02d:%02d:00]", i * 20 / 60, i * 20 % 60);
            String stop = String.format("[%02d:%02d:00]", (i * 20 + 10) / 60, (i * 20 + 10) % 60);
            gzLines(name, start + " [main/INFO]: Setting user: iCJH",
                    start + " [Render thread/INFO]: Connecting to s" + i + ".net, 25565",
                    stop + " [Render thread/INFO]: Stopping!");
            expected.add(name);
        }
        List<FileRecord> records = loadAs("iCJH");
        assertEquals(expected, records.stream().map(FileRecord::fileName).toList());
        assertEquals(60, com.wrapped.stats.YearStatsBuilder.build(2026, "iCJH", records, TODAY).uniqueServers());
        assertTrue(Files.readString(dir.resolve("wrapped").resolve("cache.json")).contains("2026-02-01-60.log.gz"));
    }
}
