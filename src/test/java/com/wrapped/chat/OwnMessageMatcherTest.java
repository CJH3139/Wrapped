package com.wrapped.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class OwnMessageMatcherTest {
    @Test
    void senderToken() {
        assertEquals(Optional.of("Bob"), OwnMessageMatcher.sender("Guild > Bob: hi"));
        assertEquals(Optional.of("Bob"), OwnMessageMatcher.sender("<Bob> hi"));
        assertEquals(Optional.empty(), OwnMessageMatcher.sender("iCJH was slain by Zombie"));
    }

    @Test
    void rankedLine() {
        assertEquals(Optional.of("me"), OwnMessageMatcher.ownMessage("[25\u2042] MINER iCJH: me", "iCJH"));
    }

    @Test
    void angleBrackets() {
        assertEquals(Optional.of("hello"), OwnMessageMatcher.ownMessage("<iCJH> hello", "iCJH"));
    }

    @Test
    void otherPlayerWithSimilarName() {
        assertEquals(Optional.empty(), OwnMessageMatcher.ownMessage("PvP \u00bb iCJHx: hi", "iCJH"));
    }

    @Test
    void nameInsideBodyIsNotSender() {
        assertEquals(Optional.empty(), OwnMessageMatcher.ownMessage("yoyo: iCJH: hi", "iCJH"));
    }

    @Test
    void prefixThenSender() {
        assertEquals(Optional.of("hi"), OwnMessageMatcher.ownMessage("PvP \u00bb iCJH: hi", "iCJH"));
    }

    @Test
    void bulletAndPipeSeparators() {
        assertEquals(Optional.of("bosses"), OwnMessageMatcher.ownMessage("ᴍᴇᴍʙᴇʀ Clatato • bosses", "Clatato"));
        assertEquals(Optional.of("im stuck"), OwnMessageMatcher.ownMessage("Clatato | im stuck", "Clatato"));
        assertEquals(Optional.of("cjh"), OwnMessageMatcher.ownMessage("STAFF | Clatato: cjh", "Clatato"));
        assertEquals(Optional.of("hey"), OwnMessageMatcher.ownMessage("Clatato → hey", "Clatato"));
        assertEquals(Optional.empty(), OwnMessageMatcher.ownMessage("OWNER iCJH • @Clatato", "Clatato"));
    }

    @Test
    void commandEchoIsNotAMessage() {
        assertEquals(Optional.empty(), OwnMessageMatcher.ownMessage("COMMAND SPY » Clatato: /tp @s icjh", "Clatato"));
    }
}
