package com.wrapped.store;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class AppDirs {
    private AppDirs() {
    }

    public static Path dataDir() {
        return dataDir(System.getProperty("os.name"), System.getenv(), System.getProperty("user.home"));
    }

    public static Path dataDir(String osName, Map<String, String> env, String userHome) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String appData = env.get("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "wrapped");
            }
            return Path.of(userHome, "AppData", "Roaming", "wrapped");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return Path.of(userHome, "Library", "Application Support", "wrapped");
        }
        String xdg = env.get("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "wrapped");
        }
        return Path.of(userHome, ".local", "share", "wrapped");
    }
}
