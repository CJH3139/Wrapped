package com.wrapped.store;

import com.wrapped.stats.FileRecord;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public final class TotalStore {
    static final int VERSION = 2;

    static final class Data {
        int version = VERSION;
        Map<String, FileRecord> records = new TreeMap<>();
        Set<String> mergedInstances = new TreeSet<>();
        Set<String> declinedInstances = new TreeSet<>();
    }

    private final Path file;
    private final Data data;

    private TotalStore(Path file, Data data) {
        this.file = file;
        this.data = data;
    }

    public static TotalStore load(Path dataDir, UUID uuid) {
        Path file = dataDir.resolve("total").resolve(uuid + ".json");
        Data data = Json.read(file, Data.class)
                .filter(d -> d.version == VERSION)
                .orElseGet(Data::new);
        data.records = data.records == null ? new TreeMap<>() : new TreeMap<>(data.records);
        data.records.values().removeIf(r -> r == null);
        data.mergedInstances = data.mergedInstances == null ? new TreeSet<>() : new TreeSet<>(data.mergedInstances);
        data.declinedInstances = data.declinedInstances == null ? new TreeSet<>() : new TreeSet<>(data.declinedInstances);
        return new TotalStore(file, data);
    }

    static String key(Path gameDir) {
        String s = gameDir.toAbsolutePath().normalize().toString();
        return File.separatorChar == '\\' ? s.toLowerCase(Locale.ROOT) : s;
    }

    public boolean shouldAsk(Path gameDir) {
        String k = key(gameDir);
        return !data.mergedInstances.contains(k) && !data.declinedInstances.contains(k);
    }

    public boolean isMerged(Path gameDir) {
        return data.mergedInstances.contains(key(gameDir));
    }

    public boolean merge(Path gameDir, Collection<FileRecord> records) {
        boolean changed = false;
        for (FileRecord r : records) {
            if (!r.latest() && r.fingerprint() != null) {
                FileRecord old = data.records.put(r.fingerprint(), r);
                changed |= !r.equals(old);
            }
        }
        String k = key(gameDir);
        changed |= data.mergedInstances.add(k);
        changed |= data.declinedInstances.remove(k);
        return changed;
    }

    public void decline(Path gameDir) {
        String k = key(gameDir);
        if (!data.mergedInstances.contains(k)) {
            data.declinedInstances.add(k);
        }
    }

    public Collection<FileRecord> records() {
        return new ArrayList<>(data.records.values());
    }

    public Collection<FileRecord> view(Collection<FileRecord> liveInstanceRecords) {
        Map<String, FileRecord> out = new LinkedHashMap<>(data.records);
        for (FileRecord r : liveInstanceRecords) {
            out.putIfAbsent(r.fingerprint(), r);
        }
        return new ArrayList<>(out.values());
    }

    public boolean save() {
        try {
            Json.write(file, data);
            return true;
        } catch (IOException | RuntimeException e) {
            System.getLogger("wrapped").log(System.Logger.Level.WARNING, "Could not save " + file, e);
            return false;
        }
    }
}
