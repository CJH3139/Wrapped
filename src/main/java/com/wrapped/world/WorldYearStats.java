package com.wrapped.world;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class WorldYearStats {
    private static final System.Logger LOGGER = System.getLogger("wrapped");

    private WorldYearStats() {
    }

    public static Optional<WorldStats> forYear(int year, Path savesDir, Path snapshotsDir, UUID uuid,
                                               Predicate<String> isBlockItem) {
        return forYear(year, savesDir, snapshotsDir, uuid, isBlockItem, WorldYearStats::creationTime);
    }

    public static Optional<WorldStats> forYear(int year, Path savesDir, Path snapshotsDir, UUID uuid,
                                               Predicate<String> isBlockItem,
                                               Function<Path, Optional<Instant>> creationTime) {
        if (!Files.isDirectory(savesDir)) {
            return Optional.empty();
        }
        List<Path> worlds = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(savesDir)) {
            dirs.filter(Files::isDirectory).forEach(worlds::add);
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not list worlds in " + savesDir, e);
            return Optional.empty();
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = start.plusYears(1);
        WorldSnapshots snapshots = new WorldSnapshots(snapshotsDir);
        Totals totals = new Totals();
        boolean any = false;
        for (Path world : worlds) {
            Optional<WorldStatsReader.Detailed> detailed =
                    worldYear(world, snapshots, uuid, isBlockItem, creationTime, zone, start, end);
            if (detailed.isPresent()) {
                totals.add(detailed.get());
                any = true;
            }
        }
        return any ? Optional.of(totals.toStats()) : Optional.empty();
    }

    private static Optional<WorldStatsReader.Detailed> worldYear(Path world, WorldSnapshots snapshots, UUID uuid,
                                                                 Predicate<String> isBlockItem,
                                                                 Function<Path, Optional<Instant>> creationTime,
                                                                 ZoneId zone, LocalDateTime start,
                                                                 LocalDateTime end) {
        List<WorldSnapshots.Snapshot> all = snapshots.list(world.getFileName().toString());
        WorldSnapshots.Snapshot lastInYear = null;
        WorldSnapshots.Snapshot lastBefore = null;
        WorldSnapshots.Snapshot firstOverall = all.isEmpty() ? null : all.get(0);
        for (WorldSnapshots.Snapshot s : all) {
            if (s.time().isBefore(start)) {
                lastBefore = s;
            } else if (s.time().isBefore(end)) {
                lastInYear = s;
            }
        }
        Optional<LocalDateTime> created = creationTime.apply(world).map(i -> LocalDateTime.ofInstant(i, zone));
        boolean createdInYear = created.isPresent() && inRange(created.get(), start, end);
        if (lastInYear != null) {
            Optional<WorldStatsReader.Detailed> later = WorldStatsReader.readDetailed(lastInYear.file(), isBlockItem);
            if (later.isPresent()) {
                if (lastBefore != null) {
                    Optional<WorldStatsReader.Detailed> earlier =
                            WorldStatsReader.readDetailed(lastBefore.file(), isBlockItem);
                    if (earlier.isPresent()) {
                        return Optional.of(subtract(later.get(), earlier.get()));
                    }
                } else if (firstOverall != null && inRange(firstOverall.time(), start, end) && createdInYear) {
                    return later;
                }
            }
        }
        Path stats = world.resolve("stats").resolve(uuid + ".json");
        if (!createdInYear || !Files.isRegularFile(stats)) {
            return Optional.empty();
        }
        try {
            LocalDateTime modified = LocalDateTime.ofInstant(Files.getLastModifiedTime(stats).toInstant(), zone);
            if (!inRange(modified, start, end)) {
                return Optional.empty();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not read modified time of " + stats, e);
            return Optional.empty();
        }
        return WorldStatsReader.readDetailed(stats, isBlockItem);
    }

    private static boolean inRange(LocalDateTime t, LocalDateTime start, LocalDateTime end) {
        return !t.isBefore(start) && t.isBefore(end);
    }

    private static Optional<Instant> creationTime(Path world) {
        try {
            return Optional.of(Files.readAttributes(world, BasicFileAttributes.class).creationTime().toInstant());
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not read creation time of " + world, e);
            return Optional.empty();
        }
    }

    private static WorldStatsReader.Detailed subtract(WorldStatsReader.Detailed later,
                                                      WorldStatsReader.Detailed earlier) {
        Map<String, Long> killers = new HashMap<>();
        later.killedBy().forEach((k, v) -> {
            long diff = v - earlier.killedBy().getOrDefault(k, 0L);
            if (diff > 0) {
                killers.put(k, diff);
            }
        });
        WorldStats a = later.stats();
        WorldStats b = earlier.stats();
        WorldStats result = new WorldStats(
                clamp(a.blocksMined() - b.blocksMined()),
                clamp(a.blocksPlaced() - b.blocksPlaced()),
                clamp(a.damageDealt() - b.damageDealt()),
                clamp(a.damageTaken() - b.damageTaken()),
                clamp(a.mobKills() - b.mobKills()),
                clamp(a.deaths() - b.deaths()),
                WorldStatsReader.topKiller(killers));
        return new WorldStatsReader.Detailed(result, killers);
    }

    private static long clamp(long v) {
        return Math.max(0, v);
    }

    private static final class Totals {
        long mined;
        long placed;
        long dealt;
        long taken;
        long kills;
        long deaths;
        final Map<String, Long> killers = new HashMap<>();

        void add(WorldStatsReader.Detailed d) {
            WorldStats s = d.stats();
            mined += s.blocksMined();
            placed += s.blocksPlaced();
            dealt += s.damageDealt();
            taken += s.damageTaken();
            kills += s.mobKills();
            deaths += s.deaths();
            d.killedBy().forEach((k, v) -> killers.merge(k, v, Long::sum));
        }

        WorldStats toStats() {
            return new WorldStats(mined, placed, dealt, taken, kills, deaths, WorldStatsReader.topKiller(killers));
        }
    }
}
