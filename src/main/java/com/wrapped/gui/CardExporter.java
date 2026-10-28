package com.wrapped.gui;

import com.wrapped.compat.Compat;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;

final class CardExporter {
    private static final System.Logger LOGGER = System.getLogger("wrapped");
    private static final long STALE_MS = 10_000;
    private static final AtomicLong STARTED = new AtomicLong();
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Wrapped card export");
        t.setDaemon(true);
        return t;
    });

    private CardExporter() {
    }

    static boolean busy() {
        long started = STARTED.get();
        return started != 0 && now() - started < STALE_MS;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    static Path export(Minecraft client, int cardX, int cardY, int cardW, int cardH, int year, String cardName) {
        int scale = Math.max(1, Compat.guiScale(client));
        Path dir = ExportName.directory(client.gameDirectory.toPath());
        Path file = ExportName.resolve(dir, year, cardName, LocalDateTime.now());
        Rect card = new Rect(cardX, cardY, cardW, cardH);
        STARTED.set(Math.max(1, now()));
        try {
            Compat.grabFrame(client, WRITER, pixels -> write(client, pixels, card, scale, dir, file));
        } catch (RuntimeException e) {
            fail(client, file, e);
        }
        return file;
    }

    private static void write(Minecraft client, Compat.Pixels pixels, Rect card, int scale, Path dir, Path file) {
        try {
            Rect crop = ExportImage.crop(card, scale, pixels.width(), pixels.height());
            if (crop.w() <= 0 || crop.h() <= 0) {
                throw new IOException("Card is outside the frame");
            }
            int[] out = ExportImage.scale(pixels.argb(), pixels.width(), crop, scale, ExportImage.WIDTH,
                    ExportImage.HEIGHT, Panels.FACE);
            Files.createDirectories(dir);
            Compat.writePng(out, ExportImage.WIDTH, ExportImage.HEIGHT, file);
            STARTED.set(0);
            Compat.showSaved(client, "Saved Wrapped card as ", file);
        } catch (IOException | RuntimeException e) {
            fail(client, file, e);
        }
    }

    private static void fail(Minecraft client, Path file, Exception e) {
        STARTED.set(0);
        LOGGER.log(Level.WARNING, "Could not save " + file, e);
        Compat.showMessage(client, "Could not save Wrapped card");
    }
}
