package com.wrapped.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogReaderTest {
    @TempDir
    Path dir;

    @Test
    void corruptGzipReturnsEmpty() throws IOException {
        byte[] junk = new byte[200];
        new Random(1).nextBytes(junk);
        Path f = dir.resolve("x.log.gz");
        Files.write(f, junk);
        assertTrue(LogReader.readLines(f).isEmpty());
    }

    @Test
    void missingFileReturnsEmpty() {
        assertTrue(LogReader.readLines(dir.resolve("none.log")).isEmpty());
    }

    @Test
    void readsGzipAndPlain() throws IOException {
        Path plain = dir.resolve("latest.log");
        Files.writeString(plain, "a\nb\n", StandardCharsets.UTF_8);
        assertEquals(List.of("a", "b"), LogReader.readLines(plain).orElseThrow());
        Path gz = dir.resolve("2026-01-01-1.log.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write("c\nd\n".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(List.of("c", "d"), LogReader.readLines(gz).orElseThrow());
    }

    @Test
    void fingerprintIsDigestOfDecompressedBytesSoPlainAndGzipMatch() throws IOException {
        byte[] content = "[10:00:00] [main/INFO]: x\nlast".getBytes(StandardCharsets.UTF_8);
        Path plain = dir.resolve("latest.log");
        Files.write(plain, content);
        Path gz = dir.resolve("2026-01-01-1.log.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write(content);
        }
        List<String> seen = new java.util.ArrayList<>();
        String a = LogReader.stream(plain, seen::add).orElseThrow();
        String b = LogReader.stream(gz, line -> {
        }).orElseThrow();
        assertEquals(a, b);
        assertEquals(java.util.HexFormat.of().formatHex(LogReader.sha256().digest(content)), a);
        assertEquals(List.of("[10:00:00] [main/INFO]: x", "last"), seen);
    }

    @Test
    void malformedBytesDoNotFail() throws IOException {
        Path f = dir.resolve("bad.log");
        Files.write(f, new byte[] {'a', (byte) 0xC2, (byte) 0xFF, '\n', 'b'});
        assertEquals(2, LogReader.readLines(f).orElseThrow().size());
    }
}
