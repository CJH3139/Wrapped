package com.wrapped.sessions;

import java.util.Locale;

public final class HostNormalizer {
    private HostNormalizer() {
    }

    public static String normalize(String host) {
        String h = host.trim().toLowerCase(Locale.ROOT);
        while (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        return h;
    }

    public static boolean isLocal(String normalizedHost) {
        String h = normalizedHost;
        if (h.equals("localhost") || h.equals("::1") || h.startsWith("127.") || h.startsWith("192.168.") || h.startsWith("10.")) {
            return true;
        }
        if (h.startsWith("172.")) {
            String[] parts = h.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
