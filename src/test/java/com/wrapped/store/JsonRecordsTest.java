package com.wrapped.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.Gson;
import com.wrapped.sessions.Session;
import com.wrapped.stats.ChatSummary;
import com.wrapped.stats.FileRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class JsonRecordsTest {
    private static FileRecord sample() {
        Session a = new Session("iCJH", Session.Target.server("minehut.com"), LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 12, 30));
        Session b = new Session("iCJH", Session.Target.world(), LocalDateTime.of(2026, 3, 2, 10, 0),
                LocalDateTime.of(2026, 3, 2, 11, 0));
        ChatSummary chat = new ChatSummary(3, Map.of("diamonds", 2), 1, 2, Map.of("Zombie", 1));
        return new FileRecord("2026-03-01-1.log.gz", false, "abc", LocalDate.of(2026, 3, 1), List.of(a, b),
                Map.of("iCJH", Map.of(2026, chat)));
    }

    private static InstanceCache.Data cache() {
        InstanceCache.Data d = new InstanceCache.Data();
        d.files = new TreeMap<>(Map.of("2026-03-01-1.log.gz", new InstanceCache.Entry(10, 20, sample())));
        return d;
    }

    private static boolean builtInRecordsWork(Gson plain) {
        try {
            return new Settings(true, false).equals(plain.fromJson(plain.toJson(new Settings(true, false)), Settings.class));
        } catch (RuntimeException | AssertionError e) {
            return false;
        }
    }

    @Test
    void writesExactlyWhatGsonsBuiltInRecordSupportWrites() {
        Gson plain = Json.base().create();
        assumeTrue(builtInRecordsWork(plain));
        assertEquals(plain.toJson(sample()), Json.GSON.toJson(sample()));
        assertEquals(plain.toJson(new Settings(true, false)), Json.GSON.toJson(new Settings(true, false)));
        assertEquals(plain.toJson(cache()), Json.GSON.toJson(cache()));
    }

    @Test
    void readsFilesWrittenByGsonsBuiltInRecordSupport() {
        Gson plain = Json.base().create();
        assumeTrue(builtInRecordsWork(plain));
        assertEquals(sample(), Json.GSON.fromJson(plain.toJson(sample()), FileRecord.class));
        InstanceCache.Data back = Json.GSON.fromJson(plain.toJson(cache()), InstanceCache.Data.class);
        assertEquals(cache().files, back.files);
        assertEquals(InstanceCache.VERSION, back.version);
    }

    @Test
    void roundTripsOnEveryGsonVersion() {
        assertEquals(sample(), Json.GSON.fromJson(Json.GSON.toJson(sample()), FileRecord.class));
        assertEquals(cache().files, Json.GSON.fromJson(Json.GSON.toJson(cache()), InstanceCache.Data.class).files);
    }

    @Test
    void writesComponentsInOrderWithoutNulls() {
        FileRecord r = new FileRecord("a.log", true, null, LocalDate.of(2026, 1, 2), List.of(), Map.of());
        String json = Json.GSON.toJson(r);
        assertTrue(json.startsWith("{\"fileName\":\"a.log\",\"latest\":true,\"date\":\"2026-01-02\",\"sessions\":[],"
                + "\"chat\":{},\"edges\":{\"leadingUnknown\":false,"), json);
        assertFalse(json.contains("fingerprint"), json);
        assertFalse(json.contains("leadStart"), json);
        assertEquals(r, Json.GSON.fromJson(json, FileRecord.class));
    }

    @Test
    void missingPrimitivesDefaultAndUnknownKeysAreIgnored() {
        assertEquals(new Settings(true, false), Json.GSON.fromJson("{\"hideTopWord\":true}", Settings.class));
        assertEquals(new Settings(false, true),
                Json.GSON.fromJson("{\"reduceMotion\":true,\"somethingNew\":[1,2]}", Settings.class));
        assertEquals(new Settings(false, false), Json.GSON.fromJson("{}", Settings.class));
    }

    @Test
    void nullJsonGivesNull() {
        assertEquals(null, Json.GSON.fromJson("null", Settings.class));
        assertFalse(Json.GSON.toJson(new Settings(false, false)).isEmpty());
    }
}
