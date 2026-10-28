package com.wrapped.store;

import java.io.IOException;
import java.nio.file.Path;

public record Settings(boolean hideTopWord, boolean reduceMotion) {
    public static Settings load(Path gameDir) {
        return Json.read(file(gameDir), Settings.class).orElse(new Settings(false, false));
    }

    public boolean save(Path gameDir) {
        Path file = file(gameDir);
        try {
            Json.write(file, this);
            return true;
        } catch (IOException | RuntimeException e) {
            System.getLogger("wrapped").log(System.Logger.Level.WARNING, "Could not save " + file, e);
            return false;
        }
    }

    private static Path file(Path gameDir) {
        return gameDir.resolve("wrapped").resolve("settings.json");
    }
}
