package com.wrapped;

import com.wrapped.cards.Card;
import com.wrapped.cards.CardDeck;
import com.wrapped.gui.WrappedData;
import com.wrapped.stats.FileRecord;
import com.wrapped.stats.YearStatsBuilder;
import com.wrapped.store.HistoryLoader;
import com.wrapped.store.InstanceCache;
import com.wrapped.store.TotalStore;
import com.wrapped.world.WorldStats;
import com.wrapped.world.WorldYearStats;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class WrappedModel {
    private static final System.Logger LOGGER = System.getLogger("wrapped");

    private record Key(int year, boolean total) {
    }

    private final Path gameDir;
    private final String name;
    private final UUID uuid;
    private final Predicate<String> isBlockItem;
    private final InstanceCache cache;
    private final TotalStore total;
    private volatile List<FileRecord> instanceRecords;
    private volatile LocalDate today;
    private final Map<Integer, Optional<WorldStats>> worlds = new ConcurrentHashMap<>();
    private volatile Map<Key, List<Card>> decks = Map.of();
    private volatile SortedSet<Integer> years;

    private WrappedModel(Path gameDir, String name, UUID uuid, Predicate<String> isBlockItem, InstanceCache cache,
            List<FileRecord> instanceRecords, TotalStore total, LocalDate today) {
        this.gameDir = gameDir;
        this.name = name;
        this.uuid = uuid;
        this.isBlockItem = isBlockItem;
        this.cache = cache;
        this.instanceRecords = instanceRecords;
        this.total = total;
        this.today = today;
    }

    public static WrappedModel load(Path gameDir, Path dataDir, String name, UUID uuid,
            Predicate<String> isBlockItem, LocalDate today) {
        InstanceCache cache = InstanceCache.load(gameDir);
        List<FileRecord> records = List.copyOf(HistoryLoader.load(gameDir, cache, today, name));
        TotalStore total = TotalStore.load(dataDir, uuid);
        if (total.isMerged(gameDir) && total.merge(gameDir, records)) {
            total.save();
        }
        WrappedModel model = new WrappedModel(gameDir, name, uuid, isBlockItem, cache, records, total, today);
        Collection<FileRecord> view = model.totalView();
        model.years = model.computeYears(view);
        model.decks = Map.copyOf(model.buildAll(model.years, true, view));
        return model;
    }

    public void refresh(LocalDate now) {
        List<FileRecord> records = List.copyOf(HistoryLoader.load(gameDir, cache, now, name));
        Collection<FileRecord> view;
        boolean save;
        synchronized (this) {
            today = now;
            instanceRecords = records;
            save = total.isMerged(gameDir) && total.merge(gameDir, records);
            view = total.view(records);
        }
        if (save) {
            total.save();
        }
        int year = now.getYear();
        worlds.remove(year);
        SortedSet<Integer> ys = computeYears(view);
        Map<Key, List<Card>> next = new HashMap<>(decks);
        for (int y : ys) {
            if (y == year || !next.containsKey(new Key(y, false))) {
                build(y, records).ifPresent(d -> next.put(new Key(y, false), d));
            }
            if (y == year || !next.containsKey(new Key(y, true))) {
                build(y, view).ifPresent(d -> next.put(new Key(y, true), d));
            }
        }
        decks = Map.copyOf(next);
        years = ys;
    }

    public static Optional<WrappedModel> tryLoad(Callable<WrappedModel> loader) {
        try {
            return Optional.ofNullable(loader.call());
        } catch (Exception | LinkageError e) {
            LOGGER.log(Level.WARNING, "Could not load Wrapped history", e);
            return Optional.empty();
        }
    }

    public static WrappedData empty() {
        return new WrappedData(Collections.unmodifiableSortedSet(new TreeSet<>()), y -> List.of(), y -> List.of());
    }

    private synchronized Collection<FileRecord> totalView() {
        return total.view(instanceRecords);
    }

    private SortedSet<Integer> computeYears(Collection<FileRecord> view) {
        SortedSet<Integer> out = new TreeSet<>(YearStatsBuilder.years(name, instanceRecords));
        out.addAll(YearStatsBuilder.years(name, view));
        return Collections.unmodifiableSortedSet(out);
    }

    private Map<Key, List<Card>> buildAll(SortedSet<Integer> ys, boolean withInstance, Collection<FileRecord> view) {
        Map<Key, List<Card>> out = new HashMap<>();
        List<FileRecord> records = instanceRecords;
        for (int year : ys) {
            if (withInstance) {
                build(year, records).ifPresent(d -> out.put(new Key(year, false), d));
            }
            build(year, view).ifPresent(d -> out.put(new Key(year, true), d));
        }
        return out;
    }

    public SortedSet<Integer> years() {
        return years;
    }

    public synchronized boolean shouldAskMerge() {
        return total.shouldAsk(gameDir);
    }

    public void answerMerge(boolean yes) {
        Collection<FileRecord> view;
        synchronized (this) {
            if (yes) {
                total.merge(gameDir, instanceRecords);
            } else {
                total.decline(gameDir);
            }
            view = total.view(instanceRecords);
        }
        total.save();
        SortedSet<Integer> ys = computeYears(view);
        Map<Key, List<Card>> rebuilt = buildAll(ys, false, view);
        Map<Key, List<Card>> next = new HashMap<>(decks);
        next.putAll(rebuilt);
        for (int year : ys) {
            if (!next.containsKey(new Key(year, false))) {
                build(year, instanceRecords).ifPresent(d -> next.put(new Key(year, false), d));
            }
        }
        decks = Map.copyOf(next);
        years = ys;
    }

    public Optional<List<Card>> built(int year, boolean useTotal) {
        return Optional.ofNullable(decks.get(new Key(year, useTotal)));
    }

    public List<Card> deck(int year, boolean useTotal) {
        return built(year, useTotal).orElseGet(() -> List.of(new Card.Title(year)));
    }

    private Optional<List<Card>> build(int year, Collection<FileRecord> source) {
        try {
            Optional<WorldStats> world = worlds.computeIfAbsent(year, y -> WorldYearStats.forYear(y,
                    gameDir.resolve("saves"), gameDir.resolve("wrapped").resolve("snapshots"), uuid, isBlockItem));
            long seed = (long) year * 31 + uuid.hashCode();
            return Optional.of(List.copyOf(CardDeck.build(YearStatsBuilder.build(year, name, source, today), world, seed)));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not build Wrapped deck for " + year, e);
            return Optional.empty();
        }
    }

    public WrappedData data() {
        return new WrappedData(years(), y -> deck(y, false), y -> deck(y, true));
    }
}
