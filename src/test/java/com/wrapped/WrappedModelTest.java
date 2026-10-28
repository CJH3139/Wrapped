package com.wrapped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrapped.cards.Card;
import com.wrapped.gui.WrappedData;
import com.wrapped.store.TotalStore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WrappedModelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private static final String NAME = "iCJH";
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path dir;

    private Path instance(String name) throws IOException {
        return Files.createDirectories(dir.resolve(name).resolve("logs")).getParent();
    }

    private Path data() {
        return dir.resolve("data");
    }

    private static void log(Path gameDir, String date, String host) throws IOException {
        List<String> lines = List.of(
                "[10:00:00] [main/INFO]: Setting user: " + NAME,
                "[10:00:05] [Render thread/INFO]: Connecting to " + host + ", 25565",
                "[10:01:00] [Render thread/INFO]: [CHAT] <" + NAME + "> hello " + date,
                "[13:30:05] [Render thread/INFO]: Stopping!");
        Path f = gameDir.resolve("logs").resolve(date + "-1.log.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(f))) {
            out.write((String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    private WrappedModel load(Path gameDir) {
        return WrappedModel.load(gameDir, data(), NAME, ID, k -> false, TODAY);
    }

    @Test
    void everyYearAndModeIsBuiltDuringLoad() throws IOException {
        Path other = instance("other");
        log(other, "2024-05-01", "hypixel.net");
        load(other).answerMerge(true);
        Path here = instance("here");
        log(here, "2025-03-01", "minehut.com");
        log(here, "2026-03-01", "minehut.com");
        WrappedModel model = load(here);
        assertEquals(Set.of(2024, 2025, 2026), model.years());
        for (int year : model.years()) {
            assertTrue(model.built(year, false).isPresent(), "instance " + year);
            assertTrue(model.built(year, true).isPresent(), "total " + year);
        }
    }

    @Test
    void failedDeckBuildFallsBackToTitleAndIsNotMemoized() throws IOException {
        int year = LocalDate.now().getYear();
        Path here = instance("here");
        log(here, year + "-01-02", "minehut.com");
        Path stats = Files.createDirectories(here.resolve("saves").resolve("W").resolve("stats"))
                .resolve(ID + ".json");
        Files.writeString(stats, "{\"stats\":{\"minecraft:used\":{\"minecraft:stone\":5}}}");
        WrappedModel model = WrappedModel.load(here, data(), NAME, ID, k -> {
            throw new IllegalStateException("registry");
        }, LocalDate.now());
        assertTrue(model.built(year, false).isEmpty());
        assertEquals(List.of(new Card.Title(year)), model.deck(year, false));
        assertEquals(List.of(new Card.Title(year)), model.data().totalDeck().apply(year));
    }

    @Test
    void yearsUnionInstanceAndTotal() throws IOException {
        Path other = instance("other");
        log(other, "2024-05-01", "hypixel.net");
        load(other).answerMerge(true);
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        WrappedModel model = load(here);
        assertEquals(Set.of(2024, 2026), model.years());
        assertEquals(Set.of(2024, 2026), model.data().years());
    }

    @Test
    void freshInstanceAsksOnceAndYesMergesAndSaves() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        WrappedModel model = load(here);
        assertTrue(model.shouldAskMerge());
        model.answerMerge(true);
        assertFalse(model.shouldAskMerge());
        TotalStore stored = TotalStore.load(data(), ID);
        assertTrue(stored.isMerged(here));
        assertEquals(1, stored.records().size());
        assertFalse(load(here).shouldAskMerge());
    }

    @Test
    void noDeclinesAndStoresNothing() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        load(here).answerMerge(false);
        TotalStore stored = TotalStore.load(data(), ID);
        assertFalse(stored.shouldAsk(here));
        assertFalse(stored.isMerged(here));
        assertEquals(0, stored.records().size());
        assertFalse(load(here).shouldAskMerge());
    }

    @Test
    void mergedInstancePullsInNewLogsOnLoad() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        load(here).answerMerge(true);
        log(here, "2026-03-02", "minehut.com");
        load(here);
        assertEquals(2, TotalStore.load(data(), ID).records().size());
    }

    @Test
    void declinedInstanceIsNotMergedOnLoad() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        load(here).answerMerge(false);
        log(here, "2026-03-02", "minehut.com");
        load(here);
        assertEquals(0, TotalStore.load(data(), ID).records().size());
    }

    @Test
    void decksAreMemoizedPerYearAndMode() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        WrappedModel model = load(here);
        List<Card> a = model.deck(2026, false);
        assertSame(a, model.built(2026, false).orElseThrow());
        assertSame(a, model.deck(2026, false));
        assertSame(a, model.data().instanceDeck().apply(2026));
        List<Card> t = model.deck(2026, true);
        assertSame(t, model.deck(2026, true));
        assertNotSame(a, t);
        assertEquals(new Card.Title(2026), a.get(0));
    }

    @Test
    void answeringRebuildsTotalDecksOnly() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        WrappedModel model = load(here);
        List<Card> instance = model.deck(2026, false);
        List<Card> total = model.deck(2026, true);
        model.answerMerge(true);
        assertSame(instance, model.deck(2026, false));
        assertNotSame(total, model.deck(2026, true));
    }

    @Test
    void totalDeckIncludesOtherInstances() throws IOException {
        Path other = instance("other");
        log(other, "2026-01-05", "hypixel.net");
        load(other).answerMerge(true);
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        WrappedModel model = load(here);
        long instanceHours = hours(model.deck(2026, false));
        long totalHours = hours(model.deck(2026, true));
        assertEquals(4, instanceHours);
        assertEquals(7, totalHours);
    }

    private static long hours(List<Card> deck) {
        return deck.stream().filter(c -> c instanceof Card.HoursReveal)
                .map(c -> ((Card.HoursReveal) c).hours()).findFirst().orElse(-1L);
    }

    @Test
    void failedLoadYieldsEmptyData() {
        Optional<WrappedModel> model = WrappedModel.tryLoad(() -> {
            throw new IllegalStateException("boom");
        });
        assertTrue(model.isEmpty());
        WrappedData empty = WrappedModel.empty();
        assertTrue(empty.years().isEmpty());
        assertTrue(empty.instanceDeck().apply(2026).isEmpty());
        assertTrue(empty.totalDeck().apply(2026).isEmpty());
    }

    @Test
    void missingLogsFolderStillLoads() {
        WrappedModel model = load(dir.resolve("nothing"));
        assertTrue(model.years().isEmpty());
        assertTrue(model.shouldAskMerge());
    }

    private static void latest(Path gameDir, String... lines) throws IOException {
        Files.write(gameDir.resolve("logs").resolve("latest.log"), List.of(lines), StandardCharsets.UTF_8);
    }

    @Test
    void refreshRebuildsCurrentYearFromLatestLog() throws IOException {
        Path here = instance("here");
        log(here, "2025-03-01", "minehut.com");
        latest(here, "[10:00:00] [main/INFO]: Setting user: " + NAME,
                "[10:00:00] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[12:00:00] [Render thread/INFO]: x");
        WrappedModel model = load(here);
        List<Card> past = model.deck(2025, false);
        assertEquals(2, hours(model.deck(2026, false)));
        latest(here, "[10:00:00] [main/INFO]: Setting user: " + NAME,
                "[10:00:00] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[15:00:00] [Render thread/INFO]: x");
        model.refresh(TODAY);
        assertEquals(5, hours(model.deck(2026, false)));
        assertEquals(5, hours(model.deck(2026, true)));
        assertSame(past, model.deck(2025, false));
    }

    @Test
    void refreshAddsTheCurrentYearWhenItFirstReachesAnHour() throws IOException {
        Path here = instance("here");
        latest(here, "[10:00:00] [main/INFO]: Setting user: " + NAME,
                "[10:00:00] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[10:20:00] [Render thread/INFO]: x");
        WrappedModel model = load(here);
        assertTrue(model.years().isEmpty());
        latest(here, "[10:00:00] [main/INFO]: Setting user: " + NAME,
                "[10:00:00] [Render thread/INFO]: Connecting to minehut.com, 25565",
                "[11:20:00] [Render thread/INFO]: x");
        model.refresh(TODAY);
        assertEquals(Set.of(2026), model.years());
        assertEquals(Set.of(2026), model.data().years());
    }

    @Test
    void yearsUnderAnHourAreNotListed() throws IOException {
        Path here = instance("here");
        Path f = here.resolve("logs").resolve("2025-03-01-1.log.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(f))) {
            out.write(String.join("\n", List.of("[10:00:00] [main/INFO]: Setting user: " + NAME,
                    "[10:00:05] [Render thread/INFO]: Connecting to a.net, 25565",
                    "[10:01:00] [Render thread/INFO]: [CHAT] <" + NAME + "> hi",
                    "[10:30:05] [Render thread/INFO]: Stopping!")).getBytes(StandardCharsets.UTF_8));
        }
        log(here, "2026-03-01", "minehut.com");
        assertEquals(Set.of(2026), load(here).years());
    }

    @Test
    void mergedLoadWithNothingNewDoesNotRewriteTotal() throws IOException {
        Path here = instance("here");
        log(here, "2026-03-01", "minehut.com");
        load(here).answerMerge(true);
        Path file = data().resolve("total").resolve(ID + ".json");
        java.nio.file.attribute.FileTime old = java.nio.file.attribute.FileTime.fromMillis(1_000_000_000L);
        Files.setLastModifiedTime(file, old);
        load(here);
        assertEquals(old, Files.getLastModifiedTime(file));
        log(here, "2026-03-02", "minehut.com");
        load(here);
        assertFalse(old.equals(Files.getLastModifiedTime(file)));
    }
}
