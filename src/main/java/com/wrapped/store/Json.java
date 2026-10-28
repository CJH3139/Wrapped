package com.wrapped.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Json {
    public static final Gson GSON = base()
            .registerTypeAdapterFactory(new RecordAdapterFactory())
            .create();

    static GsonBuilder base() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (v, t, c) -> new JsonPrimitive(v.toString()))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) -> LocalDate.parse(j.getAsString()))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (v, t, c) -> new JsonPrimitive(v.toString()))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (j, t, c) -> LocalDateTime.parse(j.getAsString()))
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (v, t, c) -> new JsonPrimitive(v.toString()))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (j, t, c) -> LocalTime.parse(j.getAsString()))
                .registerTypeAdapter(Duration.class, (JsonSerializer<Duration>) (v, t, c) -> new JsonPrimitive(v.toString()))
                .registerTypeAdapter(Duration.class, (JsonDeserializer<Duration>) (j, t, c) -> Duration.parse(j.getAsString()));
    }

    private Json() {
    }

    private static final class RecordAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> token) {
            Class<? super T> raw = token.getRawType();
            if (!raw.isRecord()) {
                return null;
            }
            return (TypeAdapter<T>) new RecordAdapter<>(gson, raw);
        }
    }

    private static final class RecordAdapter<T> extends TypeAdapter<T> {
        private final RecordComponent[] components;
        private final List<TypeAdapter<Object>> adapters = new ArrayList<>();
        private final Map<String, Integer> index = new HashMap<>();
        private final Constructor<T> constructor;

        @SuppressWarnings("unchecked")
        RecordAdapter(Gson gson, Class<T> type) {
            components = type.getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                RecordComponent c = components[i];
                types[i] = c.getType();
                adapters.add((TypeAdapter<Object>) gson.getAdapter(TypeToken.get(c.getGenericType())));
                index.put(c.getName(), i);
                c.getAccessor().setAccessible(true);
            }
            try {
                constructor = type.getDeclaredConstructor(types);
                constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new JsonParseException("No canonical constructor for " + type, e);
            }
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            for (int i = 0; i < components.length; i++) {
                Object v;
                try {
                    v = components[i].getAccessor().invoke(value);
                } catch (ReflectiveOperationException e) {
                    throw new JsonIOException(e);
                }
                if (v == null) {
                    continue;
                }
                out.name(components[i].getName());
                adapters.get(i).write(out, v);
            }
            out.endObject();
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            Object[] args = new Object[components.length];
            in.beginObject();
            while (in.hasNext()) {
                Integer i = index.get(in.nextName());
                if (i == null) {
                    in.skipValue();
                    continue;
                }
                Object v = adapters.get(i).read(in);
                if (v != null || !components[i].getType().isPrimitive()) {
                    args[i] = v;
                }
            }
            in.endObject();
            for (int i = 0; i < components.length; i++) {
                if (args[i] == null && components[i].getType().isPrimitive()) {
                    args[i] = defaultValue(components[i].getType());
                }
            }
            try {
                return constructor.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new JsonParseException("Could not build " + constructor.getDeclaringClass(), e);
            }
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return '\0';
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0f;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            return 0;
        }
    }

    public static <T> Optional<T> read(Path file, Class<T> type) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return Optional.ofNullable(GSON.fromJson(reader, type));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static void write(Path file, Object value) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
