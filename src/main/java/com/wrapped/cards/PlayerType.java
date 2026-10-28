package com.wrapped.cards;

import com.wrapped.stats.ServerStat;
import com.wrapped.stats.YearStats;
import com.wrapped.world.WorldStats;
import java.util.Optional;

public enum PlayerType {
    SERVER_HOPPER,
    LOYAL_REGULAR,
    NIGHT_OWL,
    CHATTERBOX,
    BUILDER,
    MINER,
    EXPLORER;

    static final int HOPPER_MIN_SERVERS = 5;
    static final double HOPPER_MAX_TOP_SHARE = 0.5;
    static final double LOYAL_MIN_TOP_SHARE = 0.6;
    static final int CHATTERBOX_MIN_MESSAGES = 500;
    static final long BLOCKS_MIN = 1000;
    static final double NIGHT_OWL_MIN_SHARE = 0.4;

    static double nightShare(long[] secondsByHour) {
        long total = 0;
        long night = 0;
        for (int h = 0; h < secondsByHour.length; h++) {
            total += secondsByHour[h];
            if (h >= 22 || h <= 3) {
                night += secondsByHour[h];
            }
        }
        return total <= 0 ? 0 : (double) night / total;
    }

    public static PlayerType classify(YearStats stats, Optional<WorldStats> world) {
        if (nightShare(stats.secondsByHour()) >= NIGHT_OWL_MIN_SHARE) {
            return NIGHT_OWL;
        }
        if (stats.messagesSent() >= CHATTERBOX_MIN_MESSAGES) {
            return CHATTERBOX;
        }
        double total = stats.totalTime().toSeconds();
        Optional<ServerStat> top = stats.serversByTime().stream().findFirst();
        double topShare = total <= 0 ? 0 : top.map(s -> s.time().toSeconds() / total).orElse(0.0);
        if (stats.uniqueServers() >= HOPPER_MIN_SERVERS && topShare < HOPPER_MAX_TOP_SHARE) {
            return SERVER_HOPPER;
        }
        if (top.isPresent() && topShare >= LOYAL_MIN_TOP_SHARE) {
            return LOYAL_REGULAR;
        }
        if (world.isPresent()) {
            WorldStats w = world.get();
            if (w.blocksPlaced() >= BLOCKS_MIN && w.blocksPlaced() > w.blocksMined()) {
                return BUILDER;
            }
            if (w.blocksMined() >= BLOCKS_MIN) {
                return MINER;
            }
        }
        return EXPLORER;
    }
}
