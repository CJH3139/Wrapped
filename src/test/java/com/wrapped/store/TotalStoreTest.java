package com.wrapped.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.stats.FileRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TotalStoreTest {
    @TempDir
    Path dir;

    private static FileRecord rec(String fp, boolean latest) {
        return new FileRecord(fp + ".log.gz", latest, fp, LocalDate.of(2026, 3, 1), List.of(), Map.of());
    }

    @Test
    void mergeTwiceDoesNotDoubleCount() {
        TotalStore s = TotalStore.load(dir, UUID.randomUUID());
        List<FileRecord> rs = List.of(rec("a", false), rec("b", false));
        s.merge(dir.resolve("i1"), rs);
        s.merge(dir.resolve("i1"), rs);
        assertEquals(2, s.records().size());
    }

    @Test
    void overlappingInstancesCountOnce() {
        TotalStore s = TotalStore.load(dir, UUID.randomUUID());
        s.merge(dir.resolve("i1"), List.of(rec("a", false), rec("b", false)));
        s.merge(dir.resolve("i2"), List.of(rec("a", false), rec("c", false)));
        assertEquals(3, s.records().size());
    }

    @Test
    void mergeRefreshesAlreadyMergedInstance() {
        TotalStore s = TotalStore.load(dir, UUID.randomUUID());
        s.merge(dir.resolve("i1"), List.of(rec("a", false)));
        s.merge(dir.resolve("i1"), List.of(rec("a", false), rec("b", false)));
        assertEquals(2, s.records().size());
    }

    @Test
    void isMergedOnlyAfterMergeNotDecline() {
        TotalStore s = TotalStore.load(dir, UUID.randomUUID());
        assertFalse(s.isMerged(dir.resolve("i1")));
        s.merge(dir.resolve("i1"), List.of());
        s.decline(dir.resolve("i2"));
        assertTrue(s.isMerged(dir.resolve("i1")));
        assertFalse(s.isMerged(dir.resolve("i2")));
    }

    @Test
    void askOnlyForNewInstances() {
        TotalStore s = TotalStore.load(dir, UUID.randomUUID());
        assertTrue(s.shouldAsk(dir.resolve("i1")));
        assertTrue(s.shouldAsk(dir.resolve("i2")));
        s.merge(dir.resolve("i1"), List.of());
        s.decline(dir.resolve("i2"));
        assertFalse(s.shouldAsk(dir.resolve("i1")));
        assertFalse(s.shouldAsk(dir.resolve("i2")));
    }

    @Test
    void latestRecordsAreNeverStoredButAppearInView() {
        UUID id = UUID.randomUUID();
        TotalStore s = TotalStore.load(dir, id);
        FileRecord live = rec("live", true);
        s.merge(dir.resolve("i1"), List.of(rec("a", false), live));
        assertTrue(s.save());
        TotalStore reloaded = TotalStore.load(dir, id);
        assertEquals(1, reloaded.records().size());
        assertFalse(reloaded.shouldAsk(dir.resolve("i1")));
        List<FileRecord> view = List.copyOf(reloaded.view(List.of(live, rec("a", false))));
        assertEquals(2, view.size());
        assertTrue(view.stream().anyMatch(FileRecord::latest));
    }

    @Test
    void versionMismatchStartsEmpty() throws Exception {
        UUID id = UUID.randomUUID();
        Path f = dir.resolve("total").resolve(id + ".json");
        Files.createDirectories(f.getParent());
        Files.writeString(f, "{\"version\":99,\"records\":{},\"mergedInstances\":[\"x\"]}");
        assertTrue(TotalStore.load(dir, id).shouldAsk(dir.resolve("x")));
    }

    @Test
    void dataDirBranches() {
        assertEquals(Path.of("C:/Users/u/AppData/Roaming", "wrapped"),
                AppDirs.dataDir("Windows 11", Map.of("APPDATA", "C:/Users/u/AppData/Roaming"), "C:/Users/u"));
        assertEquals(Path.of("/Users/u", "Library", "Application Support", "wrapped"),
                AppDirs.dataDir("Mac OS X", Map.of(), "/Users/u"));
        assertEquals(Path.of("/xdg", "wrapped"), AppDirs.dataDir("Linux", Map.of("XDG_DATA_HOME", "/xdg"), "/home/u"));
        assertEquals(Path.of("/home/u", ".local", "share", "wrapped"), AppDirs.dataDir("Linux", Map.of(), "/home/u"));
    }
}
