package com.wrapped.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldStatsReaderTest {
    @TempDir
    Path dir;

    @Test
    void readsVanillaStats() throws IOException {
        Path file = dir.resolve("stats.json");
        Files.writeString(file, """
                {"stats":{"minecraft:custom":{"minecraft:play_time":372,"minecraft:damage_dealt":400,"minecraft:damage_taken":100,"minecraft:mob_kills":7,"minecraft:deaths":2},
                "minecraft:mined":{"minecraft:stone":10,"minecraft:dirt":5},
                "minecraft:used":{"minecraft:stone":3,"minecraft:diamond_sword":1},
                "minecraft:killed_by":{"minecraft:zombie":2,"minecraft:creeper":2,"minecraft:skeleton":1}},"DataVersion":4440}
                """);
        WorldStats stats = WorldStatsReader.read(file, "minecraft:stone"::equals).orElseThrow();
        assertEquals(15, stats.blocksMined());
        assertEquals(3, stats.blocksPlaced());
        assertEquals(400, stats.damageDealt());
        assertEquals(100, stats.damageTaken());
        assertEquals(7, stats.mobKills());
        assertEquals(2, stats.deaths());
        assertEquals(Optional.of("creeper"), stats.topKiller());
    }

    @Test
    void missingCategoriesAreZero() throws IOException {
        Path file = dir.resolve("stats.json");
        Files.writeString(file, "{\"stats\":{}}");
        WorldStats stats = WorldStatsReader.read(file, id -> true).orElseThrow();
        assertEquals(0, stats.blocksMined());
        assertTrue(stats.topKiller().isEmpty());
    }

    @Test
    void missingFileIsEmpty() {
        assertTrue(WorldStatsReader.read(dir.resolve("none.json"), id -> true).isEmpty());
    }
}
