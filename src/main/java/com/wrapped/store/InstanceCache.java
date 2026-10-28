package com.wrapped.store;

import com.wrapped.stats.FileRecord;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class InstanceCache {
    static final int VERSION = 2;

    record Entry(long size, long lastModified, FileRecord record) {
    }

    static final class Data {
        int version = VERSION;
        Map<String, Entry> files = new TreeMap<>();
    }

    private final Path file;
    private final Data data;
    private boolean dirty;

    private InstanceCache(Path file, Data data) {
        this.file = file;
        this.data = data;
    }

    public static InstanceCache load(Path gameDir) {
        Path file = gameDir.resolve("wrapped").resolve("cache.json");
        Data data = Json.read(file, Data.class)
                .filter(d -> d.version == VERSION && d.files != null)
                .orElseGet(Data::new);
        data.files = new TreeMap<>(data.files);
        data.files.values().removeIf(e -> e == null || e.record() == null);
        return new InstanceCache(file, data);
    }

    public Optional<FileRecord> get(String fileName, long size, long lastModified) {
        Entry entry = data.files.get(fileName);
        if (entry != null && entry.size() == size && entry.lastModified() == lastModified) {
            return Optional.of(entry.record());
        }
        return Optional.empty();
    }

    public void put(String fileName, long size, long lastModified, FileRecord record) {
        data.files.put(fileName, new Entry(size, lastModified, record));
        dirty = true;
    }

    public void retainOnly(Set<String> fileNames) {
        if (data.files.keySet().retainAll(fileNames)) {
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean save() {
        try {
            Json.write(file, data);
            dirty = false;
            return true;
        } catch (IOException | RuntimeException e) {
            System.getLogger("wrapped").log(System.Logger.Level.WARNING, "Could not save " + file, e);
            return false;
        }
    }
}
