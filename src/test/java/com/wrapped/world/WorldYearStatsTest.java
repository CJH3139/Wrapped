package com.wrapped.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldYearStatsTest {
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @TempDir
    Path dir;

    private static String statsWithMined(int mined) {
        return "{\"stats\":{\"minecraft:mined\":{\"minecraft:stone\":" + mined + "}}}";
    }

    private static Instant at(String iso) {
        return LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant();
    }

    private Path world(String name, int mined, String modified) throws IOException {
        Path world = dir.resolve("saves").resolve(name);
        Files.createDirectories(world.resolve("stats"));
        Path stats = world.resolve("stats").resolve(ID + ".json");
        Files.writeString(stats, statsWithMined(mined));
        Files.setLastModifiedTime(stats, FileTime.from(at(modified)));
        return world;
    }

    @Test
    void worldSpanningYearsExcluded() throws IOException {
        world("Spanning", 50, "2026-02-01T12:00:00");
        Path saves = dir.resolve("saves");
        Path snaps = dir.resolve("snaps");
        Function<Path, Optional<Instant>> created = p -> Optional.of(at("2025-06-01T12:00:00"));
        assertTrue(WorldYearStats.forYear(2025, saves, snaps, ID, id -> true, created).isEmpty());
        assertTrue(WorldYearStats.forYear(2026, saves, snaps, ID, id -> true, created).isEmpty());
    }

    @Test
    void worldInsideYearIncluded() throws IOException {
        world("Inside", 50, "2026-02-01T12:00:00");
        Function<Path, Optional<Instant>> created = p -> Optional.of(at("2026-01-10T12:00:00"));
        WorldStats stats = WorldYearStats.forYear(2026, dir.resolve("saves"), dir.resolve("snaps"), ID,
                id -> true, created).orElseThrow();
        assertEquals(50, stats.blocksMined());
    }

    @Test
    void snapshotDifference() throws IOException {
        Path world = world("Long", 160, "2026-03-01T12:00:00");
        Path snaps = dir.resolve("snaps");
        WorldSnapshots snapshots = new WorldSnapshots(snaps);
        Path stats = world.resolve("stats").resolve(ID + ".json");
        Files.writeString(stats, statsWithMined(100));
        snapshots.snapshot(world, ID, LocalDateTime.of(2025, 12, 30, 10, 0, 0));
        Files.writeString(stats, statsWithMined(160));
        snapshots.snapshot(world, ID, LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        Function<Path, Optional<Instant>> created = p -> Optional.of(at("2025-06-01T12:00:00"));
        WorldStats result = WorldYearStats.forYear(2026, dir.resolve("saves"), snaps, ID, id -> true, created)
                .orElseThrow();
        assertEquals(60, result.blocksMined());
    }

    @Test
    void snapshotListSortedAndSkipsMissingStats() throws IOException {
        Path world = world("W", 1, "2026-03-01T12:00:00");
        WorldSnapshots snapshots = new WorldSnapshots(dir.resolve("snaps"));
        snapshots.snapshot(world, ID, LocalDateTime.of(2026, 5, 1, 0, 0, 0));
        snapshots.snapshot(world, ID, LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        snapshots.snapshot(dir.resolve("saves").resolve("Nope"), ID, LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        assertEquals(2, snapshots.list("W").size());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), snapshots.list("W").get(0).time());
        assertTrue(snapshots.list("Nope").isEmpty());
    }

    @Test
    void snapshotLeavesNoTempFilesAndReplacesSameStamp() throws IOException {
        Path world = world("A", 1, "2026-03-01T12:00:00");
        Path snaps = dir.resolve("snaps");
        WorldSnapshots snapshots = new WorldSnapshots(snaps);
        LocalDateTime t = LocalDateTime.of(2026, 3, 1, 10, 0, 0);
        snapshots.snapshot(world, ID, t);
        Files.writeString(world.resolve("stats").resolve(ID + ".json"), statsWithMined(7));
        snapshots.snapshot(world, ID, t);
        try (java.util.stream.Stream<Path> files = Files.list(snaps.resolve("A"))) {
            assertEquals(java.util.List.of("2026-03-01T10-00-00.json"),
                    files.map(f -> f.getFileName().toString()).toList());
        }
        assertEquals(statsWithMined(7), Files.readString(snaps.resolve("A").resolve("2026-03-01T10-00-00.json")));
    }
}
