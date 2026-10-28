package com.wrapped.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wrapped.store.Json;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class WorldStatsReader {
    record Detailed(WorldStats stats, Map<String, Long> killedBy) {
    }

    private WorldStatsReader() {
    }

    public static Optional<WorldStats> read(Path statsJson, Predicate<String> isBlockItem) {
        return readDetailed(statsJson, isBlockItem).map(Detailed::stats);
    }

    static Optional<Detailed> readDetailed(Path statsJson, Predicate<String> isBlockItem) {
        Optional<JsonObject> root = Json.read(statsJson, JsonObject.class);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        JsonObject stats = object(root.get(), "stats");
        JsonObject custom = object(stats, "minecraft:custom");
        long mined = sum(object(stats, "minecraft:mined"), key -> true);
        long placed = sum(object(stats, "minecraft:used"), isBlockItem);
        Map<String, Long> killedBy = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object(stats, "minecraft:killed_by").entrySet()) {
            long value = number(entry.getValue());
            if (value > 0) {
                killedBy.put(entry.getKey(), value);
            }
        }
        WorldStats result = new WorldStats(
                mined,
                placed,
                number(custom.get("minecraft:damage_dealt")),
                number(custom.get("minecraft:damage_taken")),
                number(custom.get("minecraft:mob_kills")),
                number(custom.get("minecraft:deaths")),
                topKiller(killedBy));
        return Optional.of(new Detailed(result, killedBy));
    }

    static Optional<String> topKiller(Map<String, Long> killedBy) {
        return killedBy.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .min((a, b) -> {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .map(e -> strip(e.getKey()));
    }

    private static String strip(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static long sum(JsonObject object, Predicate<String> filter) {
        long total = 0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (filter.test(entry.getKey())) {
                total += number(entry.getValue());
            }
        }
        return total;
    }

    private static long number(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return 0;
        }
        return element.getAsLong();
    }
}
