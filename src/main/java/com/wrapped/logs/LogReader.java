package com.wrapped.logs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public final class LogReader {
    private static final int BUFFER = 1 << 16;

    private LogReader() {
    }

    public static Optional<String> stream(Path file, Consumer<String> lines) {
        MessageDigest digest = sha256();
        try (InputStream raw = Files.newInputStream(file);
                InputStream in = file.getFileName().toString().endsWith(".gz") ? new GZIPInputStream(raw, BUFFER) : raw;
                DigestInputStream digesting = new DigestInputStream(in, digest);
                BufferedReader reader = new BufferedReader(new InputStreamReader(digesting, StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)), BUFFER)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.accept(line);
            }
            return Optional.of(HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static Optional<List<String>> readLines(Path file) {
        List<String> lines = new ArrayList<>();
        return stream(file, lines::add).map(fingerprint -> lines);
    }

    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
