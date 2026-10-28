package com.wrapped.gui;

import com.wrapped.cards.Card;
import net.minecraft.resources.Identifier;

enum World {
    GRASS("grass_block_top", 0xFF79C05A, 0x2E1E3C0A, 0xFF8B5E3C, 0xFF62A83E, 0xFFA0703F),
    STONE("stone", 0xFFFFFFFF, 0x40141414, 0xFFFFFFFF, 0xFFC6C6C6, 0xFF8A8A8A),
    PLANKS("oak_planks", 0xFFFFFFFF, 0x33281400, 0xFFFFD84A, 0xFFFFFFFF, 0xFFB8945F),
    GOLD("gold_block", 0xFFFFFFFF, 0x1E784600, 0xFFFFD84A, 0xFFFFFFFF, 0xFFFF9A3C, 0xFFFFF3A0),
    SANDSTONE("sandstone_top", 0xFFFFFFFF, 0x33503C14, 0xFFFFFFFF, 0xFFE7D9A6, 0xFFC0392B),
    SKY(null, 0xFFFFFFFF, 0x00000000, 0xFFFFD84A, 0xFFFFFFFF),
    SNOW("snow", 0xFFFFFFFF, 0x40203040, 0xFFFF9A3C, 0xFFFFD84A, 0xFFFFFFFF),
    BOOKSHELF("bookshelf", 0xFFFFFFFF, 0x4D1E1408, 0xFFFFFFFF, 0xFFFFD84A, 0xFF7EC8E3),
    COBBLESTONE("cobblestone", 0xFFFFFFFF, 0x40141414, 0xFF5DECF5, 0xFFFFFFFF, 0xFF8A8A8A),
    NETHERRACK("netherrack", 0xFFFFFFFF, 0x4D280000, 0xFFFF5555, 0xFFFFFFFF, 0xFFFFAA00),
    PURPUR("purpur_block", 0xFFFFFFFF, 0x33281E32, 0xFFFFD84A, 0xFFFFFFFF, 0xFFE0B0FF),
    DIRT("dirt", 0xFFFFFFFF, 0x47281400, 0xFFFFD84A, 0xFF62A83E, 0xFFFFFFFF, 0xFFFF6B6B, 0xFFC6C6C6);

    private final String texture;
    private final Identifier textureId;
    private final int textureTint;
    private final int overlay;
    private final int[] particles;

    World(String texture, int textureTint, int overlay, int... particles) {
        this.texture = texture;
        this.textureId = texture == null ? null : Identifier.withDefaultNamespace("textures/block/" + texture + ".png");
        this.textureTint = textureTint;
        this.overlay = overlay;
        this.particles = particles;
    }

    String texture() {
        return texture;
    }

    Identifier textureId() {
        return textureId;
    }

    int textureTint() {
        return textureTint;
    }

    int overlay() {
        return overlay;
    }

    int[] particles() {
        return particles.clone();
    }

    static World of(Card card) {
        return switch (card) {
            case Card.Title t -> GRASS;
            case Card.Guess g -> STONE;
            case Card.HoursReveal h -> GRASS;
            case Card.ServerCount s -> PLANKS;
            case Card.Countdown c -> STONE;
            case Card.TopServer t -> GOLD;
            case Card.LongestSession l -> SANDSTONE;
            case Card.PlayTime p -> SKY;
            case Card.Streak s -> SNOW;
            case Card.ChatCard c -> BOOKSHELF;
            case Card.Blocks b -> COBBLESTONE;
            case Card.Fights f -> NETHERRACK;
            case Card.Type t -> PURPUR;
            case Card.Summary s -> DIRT;
        };
    }
}
