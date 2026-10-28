package com.wrapped.world;

import java.util.Optional;

public record WorldStats(long blocksMined, long blocksPlaced, long damageDealt, long damageTaken,
                         long mobKills, long deaths, Optional<String> topKiller) {
}
