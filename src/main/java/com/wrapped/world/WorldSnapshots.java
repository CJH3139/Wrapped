package com.wrapped.world;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class WorldSnapshots {
    public record Snapshot(LocalDateTime time, Path file) {
    }

    private static final System.Logger LOGGER = System.getLogger("wrapped");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private final Path snapshotsDir;

    public WorldSnapshots(Path snapshotsDir) {
        this.snapshotsDir = snapshotsDir;
    }

    public void snapshot(Path worldDir, UUID uuid, LocalDateTime now) {
        Path stats = worldDir.resolve("stats").resolve(uuid + ".json");
        if (!Files.isRegularFile(stats)) {
            return;
        }
        Path folder = worldDir.getFileName();
        if (folder == null) {
            return;
        }
        Path target = snapshotsDir.resolve(folder.toString()).resolve(FORMAT.format(now) + ".json");
        Path temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), "snapshot", ".tmp");
            Files.copy(stats, temp, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not write world snapshot " + target, e);
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public List<Snapshot> list(String worldFolder) {
        Path dir = snapshotsDir.resolve(worldFolder);
        List<Snapshot> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(file -> {
                String name = file.getFileName().toString();
                if (!name.endsWith(".json")) {
                    return;
                }
                try {
                    result.add(new Snapshot(
                            LocalDateTime.parse(name.substring(0, name.length() - 5), FORMAT), file));
                } catch (DateTimeParseException ignored) {
                }
            });
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not list world snapshots in " + dir, e);
        }
        result.sort(Comparator.comparing(Snapshot::time));
        return result;
    }
}
