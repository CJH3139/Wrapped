package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.wrapped.cards.Card;
import com.wrapped.cards.PlayerType;
import com.wrapped.stats.ServerStat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class WorldTest {
    private static final ServerStat SERVER = new ServerStat("a.net", Duration.ofHours(1), 1, 1,
            LocalDate.of(2026, 1, 1));

    @Test
    void everyCardHasAWorld() {
        assertEquals(World.GRASS, World.of(new Card.Title(2026)));
        assertEquals(World.STONE, World.of(new Card.Guess(10, List.of(1, 2, 3))));
        assertEquals(World.GRASS, World.of(new Card.HoursReveal(1, "x", Optional.empty(), 2025)));
        assertEquals(World.PLANKS, World.of(new Card.ServerCount(3)));
        assertEquals(World.STONE, World.of(new Card.Countdown(List.of(SERVER))));
        assertEquals(World.GOLD, World.of(new Card.TopServer(SERVER)));
        assertEquals(World.SANDSTONE, World.of(new Card.LongestSession(Duration.ofHours(1), "a.net",
                LocalDateTime.of(2026, 3, 1, 14, 0))));
        assertEquals(World.SKY, World.of(new Card.PlayTime(new long[24], "Daytime Player")));
        assertEquals(World.SNOW, World.of(new Card.Streak(4)));
        assertEquals(World.BOOKSHELF, World.of(new Card.ChatCard(4, Optional.empty())));
        assertEquals(World.COBBLESTONE, World.of(new Card.Blocks(1, 2)));
        assertEquals(World.NETHERRACK, World.of(new Card.Fights(1, OptionalLong.empty(), OptionalLong.empty(),
                OptionalLong.empty(), OptionalLong.empty(), Optional.empty())));
        assertEquals(World.PURPUR, World.of(new Card.Type(PlayerType.MINER)));
        assertEquals(World.DIRT, World.of(new Card.Summary(1, Optional.empty(), 1, PlayerType.MINER)));
    }

    @Test
    void onlySkyHasNoTexture() {
        for (World w : World.values()) {
            if (w == World.SKY) {
                assertNull(w.texture());
            } else {
                assertNotNull(w.texture());
            }
            assertNotNull(w.particles());
        }
    }

    @Test
    void tableSpotChecks() {
        assertEquals("grass_block_top", World.GRASS.texture());
        assertEquals(0xFF79C05A, World.GRASS.textureTint());
        assertEquals(0x2E1E3C0A, World.GRASS.overlay());
        assertEquals(5, World.DIRT.particles().length);
        assertEquals(0xFFFFFFFF, World.STONE.textureTint());
    }
}
