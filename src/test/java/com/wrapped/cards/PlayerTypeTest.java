package com.wrapped.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wrapped.stats.ServerStat;
import com.wrapped.stats.YearStats;
import com.wrapped.world.WorldStats;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerTypeTest {
    private static YearStats stats(List<ServerStat> servers, long[] hours, int messages) {
        return Fixtures.with(Fixtures.base(Duration.ofHours(100), servers, hours), Duration.ofHours(100), messages, 0,
                0);
    }

    private static WorldStats world(long mined, long placed) {
        return new WorldStats(mined, placed, 0, 0, 0, 0, Optional.empty());
    }

    @Test
    void nightOwl() {
        YearStats y = stats(List.of(Fixtures.server("a", 100)), Fixtures.hourly(1, 1000), 0);
        assertEquals(PlayerType.NIGHT_OWL, PlayerType.classify(y, Optional.empty()));
    }

    @Test
    void chatterbox() {
        YearStats y = stats(List.of(Fixtures.server("a", 100)), Fixtures.evenly(), 500);
        assertEquals(PlayerType.CHATTERBOX, PlayerType.classify(y, Optional.empty()));
    }

    @Test
    void serverHopper() {
        YearStats y = stats(List.of(Fixtures.server("a", 30), Fixtures.server("b", 25), Fixtures.server("c", 20),
                Fixtures.server("d", 15), Fixtures.server("e", 10)), Fixtures.evenly(), 0);
        assertEquals(PlayerType.SERVER_HOPPER, PlayerType.classify(y, Optional.empty()));
    }

    @Test
    void loyalRegular() {
        YearStats y = stats(List.of(Fixtures.server("a", 80), Fixtures.server("b", 20)), Fixtures.evenly(), 0);
        assertEquals(PlayerType.LOYAL_REGULAR, PlayerType.classify(y, Optional.empty()));
    }

    @Test
    void builder() {
        YearStats y = stats(List.of(), Fixtures.evenly(), 0);
        assertEquals(PlayerType.BUILDER, PlayerType.classify(y, Optional.of(world(2000, 5000))));
    }

    @Test
    void miner() {
        YearStats y = stats(List.of(), Fixtures.evenly(), 0);
        assertEquals(PlayerType.MINER, PlayerType.classify(y, Optional.of(world(5000, 2000))));
    }

    @Test
    void explorerIsTheFallback() {
        YearStats y = stats(List.of(), Fixtures.evenly(), 0);
        assertEquals(PlayerType.EXPLORER, PlayerType.classify(y, Optional.empty()));
        assertEquals(PlayerType.EXPLORER, PlayerType.classify(y, Optional.of(world(10, 10))));
    }
}
