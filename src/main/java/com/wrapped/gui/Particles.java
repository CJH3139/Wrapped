package com.wrapped.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class Particles {
    static final int MAX = 600;

    record Particle(float x, float y, float vx, float vy, int size, int color, float life) {
    }

    interface Visitor {
        void visit(float x, float y, int size, int color, float life);
    }

    private static final class Live {
        float x;
        float y;
        float vx;
        float vy;
        final int size;
        final int color;
        float life;
        final float decay;

        Live(float x, float y, float vx, float vy, int size, int color, float life, float decay) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.decay = decay;
        }
    }

    private final List<Live> particles = new ArrayList<>();

    void burst(float x, float y, int[] colors, int count, float power, long seed) {
        Random random = new Random(seed);
        for (int i = 0; i < count && particles.size() < MAX; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            float speed = (float) ((2 + random.nextDouble() * 7) * power);
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed - 3 * power;
            int size = 2 + random.nextInt(4);
            int color = colors[random.nextInt(colors.length)];
            particles.add(new Live(x, y, vx, vy, size, color, 1f, 0.012f));
        }
    }

    void rain(int screenW, int screenH, int[] colors, int count, long seed) {
        Random random = new Random(seed);
        for (int i = 0; i < count && particles.size() < MAX; i++) {
            float x = random.nextFloat() * screenW;
            float y = -20 + random.nextFloat() * (-0.6f * screenH + 20);
            float vx = random.nextFloat() * 2 - 1;
            float vy = 2 + random.nextFloat() * 3;
            int size = 2 + random.nextInt(4);
            int color = colors[random.nextInt(colors.length)];
            particles.add(new Live(x, y, vx, vy, size, color, 1.6f, 0.004f));
        }
    }

    void step(long dtMs, int screenH) {
        if (particles.isEmpty()) {
            return;
        }
        float f = dtMs / 16f;
        float drag = (float) Math.pow(0.985, f);
        int kept = 0;
        for (int i = 0, n = particles.size(); i < n; i++) {
            Live p = particles.get(i);
            p.vy += 0.25f * f;
            p.vx *= drag;
            p.x += p.vx * f;
            p.y += p.vy * f;
            p.life -= p.decay * f;
            if (p.life > 0 && p.y <= screenH + 40) {
                particles.set(kept++, p);
            }
        }
        particles.subList(kept, particles.size()).clear();
    }

    void each(Visitor v) {
        for (int i = 0, n = particles.size(); i < n; i++) {
            Live p = particles.get(i);
            v.visit(p.x, p.y, p.size, p.color, p.life);
        }
    }

    List<Particle> live() {
        List<Particle> out = new ArrayList<>(particles.size());
        for (Live p : particles) {
            out.add(new Particle(p.x, p.y, p.vx, p.vy, p.size, p.color, p.life));
        }
        return out;
    }

    void clear() {
        particles.clear();
    }
}
