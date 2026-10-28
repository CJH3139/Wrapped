package com.wrapped;

import com.wrapped.compat.Compat;
import com.wrapped.gui.WrappedData;
import com.wrapped.store.AppDirs;
import com.wrapped.world.WorldSnapshots;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;

public final class WrappedState {
    private static final System.Logger LOGGER = System.getLogger("wrapped");
    private static final long STOP_WAIT_MS = 30_000;
    private static final long STOP_POLL_MS = 100;
    private static final int BOUNDARY_CHECK_TICKS = 20;

    private record World(Path dir, BooleanSupplier stopped) {
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Wrapped");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicBoolean REFRESHING = new AtomicBoolean();
    private static volatile boolean started;
    private static volatile WrappedData data;
    private static volatile WrappedModel model;
    private static volatile boolean mergeAnswered;
    private static volatile Path gameDir;
    private static volatile UUID uuid;
    private static volatile World world;
    private static YearBoundary boundary;

    private WrappedState() {
    }

    public static void startLoading(Minecraft client) {
        if (started) {
            return;
        }
        started = true;
        EXECUTOR.execute(() -> load(client));
    }

    private static void load(Minecraft client) {
        WrappedData result = null;
        try {
            Optional<WrappedModel> loaded = WrappedModel.tryLoad(() -> {
                Path dir = client.gameDirectory.toPath();
                String name = Compat.accountName(client);
                UUID id = Compat.accountId(client);
                gameDir = dir;
                uuid = id;
                return WrappedModel.load(dir, AppDirs.dataDir(), name, id, Compat::isBlockItem, LocalDate.now());
            });
            model = loaded.orElse(null);
            result = loaded.map(WrappedModel::data).orElse(null);
        } finally {
            data = result != null ? result : WrappedModel.empty();
        }
    }

    public static void refresh() {
        WrappedModel m = model;
        if (m == null || !REFRESHING.compareAndSet(false, true)) {
            return;
        }
        try {
            EXECUTOR.execute(() -> {
                try {
                    m.refresh(LocalDate.now());
                    data = m.data();
                } catch (RuntimeException | OutOfMemoryError e) {
                    LOGGER.log(Level.WARNING, "Could not refresh Wrapped history", e);
                } finally {
                    REFRESHING.set(false);
                }
            });
        } catch (RuntimeException e) {
            REFRESHING.set(false);
            LOGGER.log(Level.WARNING, "Could not refresh Wrapped history", e);
        }
    }

    public static Optional<WrappedData> data() {
        return Optional.ofNullable(data);
    }

    public static boolean shouldAskMerge() {
        WrappedModel m = model;
        if (m == null || mergeAnswered) {
            return false;
        }
        try {
            return m.shouldAskMerge();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not check Total merge state", e);
            return false;
        }
    }

    public static void answerMerge(boolean yes) {
        WrappedModel m = model;
        if (m == null || mergeAnswered) {
            return;
        }
        mergeAnswered = true;
        EXECUTOR.execute(() -> {
            try {
                m.answerMerge(yes);
                data = m.data();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Could not update Total", e);
            }
        });
    }

    public static void onJoin(Minecraft client) {
        Optional<Path> dir = client.isLocalServer() ? Compat.singleplayerWorld(client) : Optional.empty();
        if (dir.isEmpty()) {
            world = null;
            return;
        }
        World w = new World(dir.get(), Compat.singleplayerStopped(client));
        world = w;
        synchronized (WrappedState.class) {
            boundary = new YearBoundary(LocalDate.now().getYear());
        }
        snapshot(w.dir(), () -> true);
    }

    public static void onDisconnect() {
        World w = world;
        world = null;
        if (w != null) {
            snapshot(w.dir(), w.stopped());
        }
    }

    public static void onTick(long tick) {
        World w = world;
        if (w == null || tick % BOUNDARY_CHECK_TICKS != 0) {
            return;
        }
        OptionalInt old;
        synchronized (WrappedState.class) {
            old = boundary == null ? OptionalInt.empty() : boundary.crossed(LocalDate.now().getYear());
        }
        if (old.isPresent()) {
            snapshot(w.dir(), () -> true, Optional.of(YearBoundary.stamp(old.getAsInt())));
        }
    }

    private static void snapshot(Path worldDir, BooleanSupplier ready) {
        snapshot(worldDir, ready, Optional.empty());
    }

    private static void snapshot(Path worldDir, BooleanSupplier ready, Optional<LocalDateTime> stamp) {
        EXECUTOR.execute(() -> {
            Path dir = gameDir;
            UUID id = uuid;
            if (dir == null || id == null) {
                return;
            }
            try {
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STOP_WAIT_MS);
                while (!ready.getAsBoolean() && System.nanoTime() < deadline) {
                    Thread.sleep(STOP_POLL_MS);
                }
                new WorldSnapshots(dir.resolve("wrapped").resolve("snapshots"))
                        .snapshot(worldDir, id, stamp.orElseGet(LocalDateTime::now));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Could not snapshot " + worldDir, e);
            }
        });
    }
}
