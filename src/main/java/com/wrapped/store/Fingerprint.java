package com.wrapped.store;

import com.wrapped.logs.LogReader;
import java.util.HexFormat;

public final class Fingerprint {
    private Fingerprint() {
    }

    public static String of(byte[] decompressed) {
        return HexFormat.of().formatHex(LogReader.sha256().digest(decompressed));
    }
}
