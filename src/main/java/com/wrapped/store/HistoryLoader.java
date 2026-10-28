package com.wrapped.store;

import com.wrapped.logs.LogFileName;
import com.wrapped.logs.LogReader;
import com.wrapped.stats.FileRecord;
import com.wrapped.stats.FileRecords;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class HistoryLoader {
    private static final System.Logger LOGGER = System.getLogger("wrapped");
    static final int SAVE_EVERY = 50;
    static final Comparator<LogFileName> ORDER = Comparator.comparing(LogFileName::date)
            .thenComparing(LogFileName::latest)
            .thenComparingInt(LogFileName::index);

    private record Candidate(Path path, String name, LogFileName parsed, long size, long modified) {
    }

    private HistoryLoader() {
    }

    public static List<FileRecord> load(Path gameDir, InstanceCache cache, LocalDate today) {
        return load(gameDir, cache, today, FileRecord.UNKNOWN);
    }

    public static List<FileRecord> load(Path gameDir, InstanceCache cache, LocalDate today, String account) {
        String hint = account == null ? FileRecord.UNKNOWN : account;
        List<Candidate> candidates = candidates(gameDir.resolve("logs"), today);
        if (candidates == null) {
            return new ArrayList<>();
        }
        List<Optional<FileRecord>> slots = new ArrayList<>();
        List<Candidate> pending = new ArrayList<>();
        List<Integer> pendingSlots = new ArrayList<>();
        Set<String> present = new HashSet<>();
        for (Candidate c : candidates) {
            if (!c.parsed().latest()) {
                Optional<FileRecord> cached = cache.get(c.name(), c.size(), c.modified())
                        .filter(r -> !r.edges().leadingUnknown() || r.edges().leadAccount().equals(hint));
                if (cached.isPresent()) {
                    present.add(c.name());
                    slots.add(cached);
                    continue;
                }
            }
            pendingSlots.add(slots.size());
            slots.add(Optional.empty());
            pending.add(c);
        }
        parseAll(pending, hint, (i, record) -> {
            Candidate c = pending.get(i);
            slots.set(pendingSlots.get(i), record);
            if (record.isPresent() && !c.parsed().latest()) {
                present.add(c.name());
                cache.put(c.name(), c.size(), c.modified(), record.get());
                return true;
            }
            return false;
        }, cache);
        cache.retainOnly(present);
        if (cache.isDirty()) {
            cache.save();
        }
        List<FileRecord> raw = new ArrayList<>();
        for (Optional<FileRecord> r : slots) {
            r.ifPresent(raw::add);
        }
        return FileRecords.stitchAll(raw);
    }

    private interface Collector {
        boolean collect(int index, Optional<FileRecord> record);
    }

    private static void parseAll(List<Candidate> pending, String hint, Collector collector, InstanceCache cache) {
        if (pending.isEmpty()) {
            return;
        }
        if (pending.size() == 1) {
            collector.collect(0, parse(pending.get(0), hint));
            return;
        }
        int saved = 0;
        ExecutorService pool = Executors.newFixedThreadPool(threads(), daemonFactory());
        try {
            List<Future<Optional<FileRecord>>> futures = new ArrayList<>();
            for (Candidate c : pending) {
                futures.add(pool.submit(() -> parse(c, hint)));
            }
            for (int i = 0; i < futures.size(); i++) {
                Optional<FileRecord> record;
                try {
                    record = futures.get(i).get();
                } catch (ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Could not read " + pending.get(i).path(), e.getCause());
                    record = Optional.empty();
                }
                if (collector.collect(i, record)) {
                    saved++;
                    if (saved % SAVE_EVERY == 0) {
                        cache.save();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
    }

    static int threads() {
        return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    }

    private static ThreadFactory daemonFactory() {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "Wrapped log reader " + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        };
    }

    private static Optional<FileRecord> parse(Candidate c, String hint) {
        try {
            FileRecords.Builder builder = new FileRecords.Builder(c.parsed().date(), hint);
            Optional<String> fingerprint = LogReader.stream(c.path(), builder::accept);
            return fingerprint.map(fp -> builder.finish(c.name(), c.parsed().latest(), fp));
        } catch (OutOfMemoryError e) {
            LOGGER.log(Level.WARNING, "Skipped " + c.path() + ": out of memory", e);
            return Optional.empty();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not read " + c.path(), e);
            return Optional.empty();
        }
    }

    private static List<Candidate> candidates(Path logs, LocalDate today) {
        List<Path> files;
        try (Stream<Path> stream = Files.list(logs)) {
            files = stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            return null;
        }
        List<Candidate> out = new ArrayList<>();
        for (Path path : files) {
            String name = path.getFileName().toString();
            Optional<LogFileName> parsed = LogFileName.parse(name, today);
            if (parsed.isEmpty()) {
                continue;
            }
            BasicFileAttributes attrs;
            try {
                attrs = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (IOException e) {
                continue;
            }
            out.add(new Candidate(path, name, parsed.get(), attrs.size(), attrs.lastModifiedTime().toMillis()));
        }
        out.sort(Comparator.comparing(Candidate::parsed, ORDER));
        return out;
    }
}
