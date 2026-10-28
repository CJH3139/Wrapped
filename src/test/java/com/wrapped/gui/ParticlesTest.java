package com.wrapped.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParticlesTest {
    private static final int[] COLORS = {0xFFFFFFFF, 0xFFFFD84A};

    @Test
    void burstFallsAndFades() {
        Particles p = new Particles();
        p.burst(100, 100, COLORS, 10, 1f, 1L);
        assertEquals(10, p.live().size());
        p.step(2000, 240);
        assertTrue(p.live().isEmpty());
    }

    @Test
    void capIsRespected() {
        Particles p = new Particles();
        p.burst(100, 100, COLORS, 1000, 1f, 1L);
        assertEquals(Particles.MAX, p.live().size());
        p.rain(320, 240, COLORS, 50, 2L);
        assertEquals(Particles.MAX, p.live().size());
    }

    @Test
    void sameSeedSameParticles() {
        Particles a = new Particles();
        Particles b = new Particles();
        a.burst(50, 60, COLORS, 20, 1.5f, 42L);
        b.burst(50, 60, COLORS, 20, 1.5f, 42L);
        assertEquals(a.live(), b.live());
    }

    @Test
    void rainStartsAboveScreenAndClearEmpties() {
        Particles p = new Particles();
        p.rain(320, 240, COLORS, 30, 7L);
        for (Particles.Particle q : p.live()) {
            assertTrue(q.y() <= -20 && q.y() >= -0.6f * 240);
            assertEquals(1.6f, q.life(), 0.0001f);
        }
        p.clear();
        assertTrue(p.live().isEmpty());
    }
}
