package com.wrapped.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuessOptionsTest {
    @Test
    void guessContainsReal() {
        List<Integer> a = GuessOptions.of(214, 7);
        assertEquals(3, a.size());
        assertEquals(3, new HashSet<>(a).size());
        assertTrue(a.contains(214));
        assertEquals(a, GuessOptions.of(214, 7));
    }

    @Test
    void decoysNearHalfAndDouble() {
        List<Integer> a = GuessOptions.of(100, 3);
        assertTrue(a.contains(50));
        assertTrue(a.contains(200));
    }

    @Test
    void smallValuesStayDistinctAndPositive() {
        for (int real = 1; real <= 6; real++) {
            for (long seed = 0; seed < 20; seed++) {
                List<Integer> a = GuessOptions.of(real, seed);
                assertEquals(3, new HashSet<>(a).size());
                assertTrue(a.contains(real));
                assertTrue(a.stream().allMatch(v -> v > 0));
            }
        }
    }

    @Test
    void seedChangesOrderSomewhere() {
        boolean differs = false;
        for (long seed = 0; seed < 20; seed++) {
            if (!GuessOptions.of(214, seed).equals(GuessOptions.of(214, 0))) {
                differs = true;
            }
        }
        assertTrue(differs);
    }

    @Test
    void relatable() {
        assertEquals("that's almost 9 days", Relatable.hours(Duration.ofHours(214)));
        assertEquals("that's 8 full days", Relatable.hours(Duration.ofHours(200)));
        assertEquals("that's 8 full days", Relatable.hours(Duration.ofHours(209)));
        assertEquals("that's almost 9 days", Relatable.hours(Duration.ofHours(210)));
        assertEquals("that's a full day", Relatable.hours(Duration.ofHours(35)));
        assertEquals("that's almost 2 days", Relatable.hours(Duration.ofHours(42)));
        assertEquals("that's 6 movies back to back", Relatable.hours(Duration.ofHours(12)));
        assertEquals("that's 11 movies back to back", Relatable.hours(Duration.ofHours(23)));
        assertEquals("that's a full day", Relatable.hours(Duration.ofHours(24)));
        assertEquals("that's 2 full days", Relatable.hours(Duration.ofHours(48)));
        assertEquals("that's 2 movies back to back", Relatable.hours(Duration.ofHours(5)));
        assertEquals("that's about one movie", Relatable.hours(Duration.ofMinutes(90)));
    }

    @Test
    void relatableOneHour() {
        assertEquals("that's about one movie", Relatable.hours(Duration.ofHours(1)));
        assertEquals("that's about one movie", Relatable.hours(Duration.ofMinutes(80)));
    }

    @Test
    void relatableUnderAnHour() {
        assertEquals("that's less than an hour", Relatable.hours(Duration.ofMinutes(59)));
        assertEquals("that's less than an hour", Relatable.hours(Duration.ZERO));
    }
}
