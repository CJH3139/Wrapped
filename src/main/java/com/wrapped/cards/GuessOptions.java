package com.wrapped.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GuessOptions {
    private GuessOptions() {
    }

    public static List<Integer> of(int realHours, long seed) {
        int real = Math.max(1, realHours);
        Set<Integer> values = new LinkedHashSet<>();
        values.add(real);
        values.add(Math.max(1, real / 2));
        values.add(real * 2);
        int next = real + 1;
        while (values.size() < 3) {
            values.add(next++);
        }
        List<Integer> list = new ArrayList<>(values);
        Collections.shuffle(list, new Random(seed));
        return List.copyOf(list);
    }
}
