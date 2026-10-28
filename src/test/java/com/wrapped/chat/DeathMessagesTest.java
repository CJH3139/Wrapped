package com.wrapped.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wrapped.chat.DeathMessages.Death;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeathMessagesTest {
    private static final String LINE = "PvP \u00bb MickleDePickleM was slain by Joninator__";

    @Test
    void playerDied() {
        assertEquals(Optional.of(new Death(true, "Joninator__")), DeathMessages.parse(LINE, "MickleDePickleM"));
    }

    @Test
    void playerKilled() {
        assertEquals(Optional.of(new Death(false, "MickleDePickleM")), DeathMessages.parse(LINE, "Joninator__"));
    }

    @Test
    void soloDeath() {
        assertEquals(Optional.of(new Death(true, "")), DeathMessages.parse("iCJH fell from a high place", "iCJH"));
    }

    @Test
    void multiWordKiller() {
        assertEquals(Optional.of(new Death(true, "Cave Spider")), DeathMessages.parse("iCJH was slain by Cave Spider", "iCJH"));
    }

    @Test
    void itemSuffixRemoved() {
        assertEquals(Optional.of(new Death(true, "Bob")),
                DeathMessages.parse("iCJH was slain by Bob using [Diamond Sword]", "iCJH"));
    }

    @Test
    void shotBy() {
        assertEquals(Optional.of(new Death(true, "Skeleton")), DeathMessages.parse("iCJH was shot by Skeleton", "iCJH"));
    }

    @Test
    void blownUpBy() {
        assertEquals(Optional.of(new Death(true, "Creeper")), DeathMessages.parse("iCJH was blown up by Creeper", "iCJH"));
    }

    @Test
    void died() {
        assertEquals(Optional.of(new Death(true, "")), DeathMessages.parse("iCJH died", "iCJH"));
    }

    @Test
    void nameAbsentIsEmpty() {
        assertEquals(Optional.empty(), DeathMessages.parse("Bob was slain by Alice", "iCJH"));
    }

    @Test
    void unrelated() {
        assertEquals(Optional.empty(), DeathMessages.parse(LINE, "someone"));
    }
}
