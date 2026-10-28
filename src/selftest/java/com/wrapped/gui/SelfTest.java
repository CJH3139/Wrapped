package com.wrapped.gui;

import com.wrapped.WrappedModel;
import com.wrapped.cards.Card;
import com.wrapped.compat.Compat;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class SelfTest implements ClientModInitializer {
    private static final String[] SERVERS = {"mc.hypixel.net", "play.minehut.com", "2b2t.org", "mc.cubecraft.net",
            "play.wynncraft.com", "mineplex.com", "play.pvplegacy.net"};
    private static final long SETTLE_MS = 3600;
    private static final int MAX_STEPS = 60;

    private final List<String> report = new ArrayList<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private Path out;
    private long startedAt = -1;
    private long phaseAt;
    private long readyAt = -1;
    private int lastIndex = -1;
    private int phase;
    private int step;
    private int shot;
    private WrappedData data;
    private WrappedScreen screen;
    private boolean failed;

    @Override
    public void onInitializeClient() {
        String dir = System.getProperty("wrapped.selftest.out");
        if (dir == null) {
            return;
        }
        out = Paths.get(dir);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private void tick(Minecraft client) {
        try {
            run(client);
        } catch (Throwable t) {
            fail("tick threw " + stack(t));
            finish(client);
        }
    }

    private void run(Minecraft client) throws Exception {
        long now = now();
        if (startedAt < 0) {
            startedAt = now;
            phaseAt = now;
            Files.createDirectories(out);
            return;
        }
        long since = now - phaseAt;
        switch (phase) {
            case 0 -> {
                if (since < 12000) {
                    return;
                }
                Class<?> world;
                try {
                    world = Class.forName("com.wrapped.gui.SelfTestWorld");
                } catch (ClassNotFoundException e) {
                    world = null;
                }
                if (world != null) {
                    world.getMethod("create", Minecraft.class).invoke(null, client);
                    report.add("creating a world first");
                    phase = 10;
                    phaseAt = now;
                    return;
                }
                startCards(client, now);
            }
            case 10 -> {
                if (client.player == null || Compat.screen(client) != null) {
                    readyAt = -1;
                    if (since > 180000) {
                        fail("world never loaded");
                        finish(client);
                    }
                    return;
                }
                if (readyAt < 0) {
                    readyAt = now;
                }
                if (now - readyAt < 6000) {
                    return;
                }
                check(true, "world loaded");
                startCards(client, now);
            }
            case 1 -> {
                if (since < 1500) {
                    return;
                }
                check(Compat.screen(client) instanceof MergePrompt, "merge prompt is open");
                capture(client, "merge-prompt");
                screen = new WrappedScreen(() -> Optional.of(data), () -> false, yes -> {
                });
                Compat.setScreen(client, screen.opening());
                next(now);
            }
            case 2 -> {
                if (since < SETTLE_MS) {
                    return;
                }
                check(Compat.screen(client) == screen, "wrapped screen stayed open");
                StageController c = controller();
                Optional<Card> card = c.card();
                check(card.isPresent(), "deck has a card at step " + step);
                String name = card.map(x -> x.getClass().getSimpleName()).orElse("none");
                if (lastIndex >= 0 && c.index() > lastIndex + 1) {
                    fail("deck jumped from card " + lastIndex + " to " + c.index() + " (outside input?)");
                }
                lastIndex = c.index();
                capture(client, String.format("%02d-%s", step, name));
                if (step == 0) {
                    screen.setTotal(true);
                    phase = 20;
                    phaseAt = now;
                    return;
                }
                if (card.isPresent() && card.get() instanceof Card.Summary) {
                    Method save = WrappedScreen.class.getDeclaredMethod("requestSave");
                    save.setAccessible(true);
                    save.invoke(screen);
                    next(now);
                    return;
                }
                if (++step > MAX_STEPS) {
                    fail("never reached the summary card");
                    finish(client);
                    return;
                }
                screen.onKey(Compat.KEY_RIGHT);
                phaseAt = now;
            }
            case 20 -> {
                if (since < 1200) {
                    return;
                }
                capture(client, "00-Title-total");
                screen.setTotal(false);
                step = 1;
                phase = 21;
                phaseAt = now;
            }
            case 21 -> {
                if (since < 800) {
                    return;
                }
                screen.onKey(Compat.KEY_RIGHT);
                phase = 2;
                phaseAt = now;
            }
            case 3 -> {
                if (since < 4000) {
                    return;
                }
                Path dir = client.gameDirectory.toPath().resolve("screenshots").resolve("wrapped");
                Path latest = null;
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> files = Files.list(dir)) {
                        latest = files.filter(p -> p.getFileName().toString().endsWith(".png"))
                                .filter(p -> {
                                    try {
                                        return Files.getLastModifiedTime(p).toMillis() >= System.currentTimeMillis() - 60000;
                                    } catch (IOException e) {
                                        return false;
                                    }
                                })
                                .findFirst().orElse(null);
                    }
                }
                check(latest != null, "save as image wrote a png");
                if (latest != null) {
                    Files.copy(latest, out.resolve("export.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    report.add("export bytes " + Files.size(latest));
                }
                finish(client);
            }
            default -> {
            }
        }
    }

    private void startCards(Minecraft client, long now) throws IOException {
        data = buildData(client);
        WrappedScreen first = new WrappedScreen(() -> Optional.of(data), () -> true, yes -> {
        });
        Compat.setScreen(client, first.opening());
        phase = 1;
        phaseAt = now;
    }

    private void next(long now) {
        phase++;
        phaseAt = now;
    }

    private StageController controller() throws ReflectiveOperationException {
        Field f = WrappedScreen.class.getDeclaredField("controller");
        f.setAccessible(true);
        return (StageController) f.get(screen);
    }

    private WrappedData buildData(Minecraft client) throws IOException {
        Path root = Files.createTempDirectory("wrapped-selftest");
        Path game = root.resolve("game");
        Path dataDir = root.resolve("data");
        String name = Compat.accountName(client);
        UUID id = Compat.accountId(client);
        int year = LocalDate.now().getYear();
        writeLogs(game.resolve("logs"), name, year);
        writeStats(game.resolve("saves").resolve("Fixture").resolve("stats").resolve(id + ".json"));
        WrappedModel model = WrappedModel.load(game, dataDir, name, id, Compat::isBlockItem, LocalDate.now());
        check(model.shouldAskMerge(), "fresh instance asks to merge");
        model.answerMerge(true);
        check(!model.shouldAskMerge(), "merged instance stops asking");
        check(Files.isRegularFile(dataDir.resolve("total").resolve(id + ".json")), "total file written");
        WrappedModel again = WrappedModel.load(game, dataDir, name, id, Compat::isBlockItem, LocalDate.now());
        check(!again.shouldAskMerge(), "total file read back with the game's Gson");
        WrappedData d = again.data();
        check(d.years().contains(year), "fixture year present: " + d.years());
        List<Card> deck = d.instanceDeck().apply(year);
        report.add("deck " + deck.size() + " cards: " + deck.stream().map(c -> c.getClass().getSimpleName()).toList());
        check(deck.size() >= 10, "deck has the full run of cards");
        return d;
    }

    private static void writeLogs(Path logs, String name, int year) throws IOException {
        Files.createDirectories(logs);
        Random r = new Random(42);
        LocalDate day = LocalDate.of(year, 1, 3);
        LocalDate stop = LocalDate.now().minusDays(1);
        int n = 0;
        while (day.isBefore(stop)) {
            StringBuilder sb = new StringBuilder();
            int t = 17 * 3600 + r.nextInt(3 * 3600);
            sb.append(line(t, "main", "Setting user: " + name));
            int sessions = 1 + r.nextInt(3);
            for (int s = 0; s < sessions; s++) {
                t += 60 + r.nextInt(600);
                boolean world = r.nextInt(5) == 0;
                if (world) {
                    sb.append(line(t, "Server thread", name + "[local:E:" + Integer.toHexString(r.nextInt()) + "] logged in with entity id 1 at (0.5, 64.0, 0.5)"));
                } else {
                    String server = SERVERS[Math.min(SERVERS.length - 1, (int) Math.floor(Math.abs(r.nextGaussian()) * 2.2))];
                    sb.append(line(t, "Render thread", "Connecting to " + server + ", 25565"));
                }
                int len = 900 + r.nextInt(5400);
                for (int k = 0; k < 1 + r.nextInt(6); k++) {
                    int at = t + r.nextInt(len);
                    String[] words = {"gg", "diamonds", "anyone want to team", "lol", "gg wp", "diamonds here", "nice"};
                    sb.append(line(at, "Render thread", "[CHAT] <" + name + "> " + words[r.nextInt(words.length)]));
                }
                if (r.nextInt(4) == 0) {
                    sb.append(line(t + len / 2, "Render thread", "[CHAT] " + name + " was slain by Zombie"));
                }
                t += len;
                if (world) {
                    sb.append(line(t, "Server thread", "Stopping singleplayer server as player logged out"));
                }
            }
            t += 30;
            sb.append(line(t, "Render thread", "Stopping!"));
            Path file = logs.resolve(day + "-1.log.gz");
            try (OutputStream o = new GZIPOutputStream(Files.newOutputStream(file))) {
                o.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            n++;
            day = day.plusDays(1 + r.nextInt(5));
        }
    }

    private static String line(int seconds, String thread, String msg) {
        int s = Math.min(seconds, 23 * 3600 + 59 * 60 + 59);
        return String.format("[%02d:%02d:%02d] [%s/INFO]: %s%n", s / 3600, s / 60 % 60, s % 60, thread, msg);
    }

    private static void writeStats(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        String json = "{\"stats\":{\"minecraft:mined\":{\"minecraft:stone\":5210,\"minecraft:dirt\":830},"
                + "\"minecraft:used\":{\"minecraft:cobblestone\":1400,\"minecraft:diamond_pickaxe\":3},"
                + "\"minecraft:killed_by\":{\"minecraft:zombie\":4,\"minecraft:creeper\":2},"
                + "\"minecraft:custom\":{\"minecraft:damage_dealt\":48210,\"minecraft:damage_taken\":19020,"
                + "\"minecraft:mob_kills\":612,\"minecraft:deaths\":9,\"minecraft:player_kills\":3}},\"DataVersion\":2566}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private void capture(Minecraft client, String label) {
        int index = shot++;
        Path file = out.resolve(String.format("%02d_%s.png", index, label));
        Compat.grabFrame(client, writer, pixels -> {
            try {
                Compat.writePng(pixels.argb(), pixels.width(), pixels.height(), file);
            } catch (IOException e) {
                fail("could not write " + file + ": " + e);
            }
        });
        report.add("shot " + file.getFileName());
    }

    private void check(boolean ok, String what) {
        report.add((ok ? "PASS " : "FAIL ") + what);
        if (!ok) {
            failed = true;
        }
    }

    private void fail(String what) {
        report.add("FAIL " + what);
        failed = true;
    }

    private static String stack(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    private void finish(Minecraft client) {
        phase = 99;
        writer.shutdown();
        try {
            writer.awaitTermination(10, TimeUnit.SECONDS);
            report.add(failed ? "RESULT FAIL" : "RESULT OK");
            Files.write(out.resolve("report.txt"), report, StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException e) {
            System.err.println("WRAPPED_SELFTEST could not write report " + e);
        }
        System.out.println("WRAPPED_SELFTEST " + (failed ? "FAIL" : "OK"));
        for (Method m : Minecraft.class.getMethods()) {
            if (m.getName().equals("stop") && m.getParameterCount() == 0) {
                try {
                    m.invoke(client);
                    return;
                } catch (ReflectiveOperationException e) {
                    break;
                }
            }
        }
        System.exit(failed ? 1 : 0);
    }
}
