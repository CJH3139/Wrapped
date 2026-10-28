package com.wrapped;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wrapped.world.WorldSnapshots;
import com.wrapped.world.WorldStats;
import com.wrapped.world.WorldYearStats;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YearBoundaryTest {
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @TempDir
    Path dir;

    @Test
    void firesOncePerNewYearWithTheOldYear() {
        YearBoundary b = new YearBoundary(2026);
        assertEquals(OptionalInt.empty(), b.crossed(2026));
        assertEquals(OptionalInt.of(2026), b.crossed(2027));
        assertEquals(OptionalInt.empty(), b.crossed(2027));
        assertEquals(OptionalInt.of(2027), b.crossed(2028));
    }

    @Test
    void stampIsLastSecondOfOldYear() {
        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59, 59), YearBoundary.stamp(2025));
    }

    @Test
    void boundarySnapshotEndsOldYearAndStartsNewYear() throws IOException {
        Path world = dir.resolve("saves").resolve("W");
        Path stats = Files.createDirectories(world.resolve("stats")).resolve(ID + ".json");
        Path snaps = dir.resolve("snaps");
        WorldSnapshots snapshots = new WorldSnapshots(snaps);
        write(stats, 5);
        snapshots.snapshot(world, ID, LocalDateTime.of(2024, 11, 1, 10, 0, 0));
        write(stats, 20);
        snapshots.snapshot(world, ID, LocalDateTime.of(2025, 6, 1, 10, 0, 0));
        write(stats, 50);
        snapshots.snapshot(world, ID, YearBoundary.stamp(2025));
        write(stats, 80);
        snapshots.snapshot(world, ID, LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        Function<Path, Optional<Instant>> created = p -> Optional.of(
                LocalDateTime.of(2024, 10, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        WorldStats old = WorldYearStats.forYear(2025, dir.resolve("saves"), snaps, ID, k -> true, created)
                .orElseThrow();
        WorldStats next = WorldYearStats.forYear(2026, dir.resolve("saves"), snaps, ID, k -> true, created)
                .orElseThrow();
        assertEquals(45, old.blocksMined());
        assertEquals(30, next.blocksMined());
    }

    private static void write(Path stats, int mined) throws IOException {
        Files.writeString(stats, "{\"stats\":{\"minecraft:mined\":{\"minecraft:stone\":" + mined + "}}}");
    }
}
