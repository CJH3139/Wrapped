package com.wrapped.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class WordCounterTest {
    @Test
    void topWord() {
        WordCounter c = new WordCounter();
        c.add("gg wp");
        c.add("gg diamonds");
        c.add("diamonds now");
        assertEquals(Optional.of("diamonds"), c.top());
        assertEquals(2, c.counts().get("diamonds"));
    }

    @Test
    void tieBreaksAlphabetically() {
        WordCounter c = new WordCounter();
        c.add("zebra apple");
        assertEquals(Optional.of("apple"), c.top());
    }

    @Test
    void skipsStopwordsAndDigits() {
        WordCounter c = new WordCounter();
        c.add("the just like abc123 you");
        assertEquals(Optional.empty(), c.top());
    }

    @Test
    void apostropheWordsAndFunctionWordsAreDropped() {
        WordCounter c = new WordCounter();
        c.add("don't it's you're they then there would yeah gonna okay");
        c.add("Don't DONT dont didn doesn");
        assertEquals(Optional.empty(), c.top());
    }

    @Test
    void slangStays() {
        WordCounter c = new WordCounter();
        c.add("lol bro lol");
        assertEquals(Optional.of("lol"), c.top());
        assertEquals(1, c.counts().get("bro"));
        org.junit.jupiter.api.Assertions.assertTrue(WordCounter.isCountable("diamonds"));
        org.junit.jupiter.api.Assertions.assertFalse(WordCounter.isCountable("it's"));
    }

    @Test
    void ownNameIsSkipped() {
        WordCounter c = new WordCounter();
        c.add("Clatato clatato come here", "Clatato");
        assertEquals(Optional.of("come"), c.top());
    }
}
