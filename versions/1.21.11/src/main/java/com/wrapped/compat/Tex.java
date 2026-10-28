package com.wrapped.compat;

import net.minecraft.resources.Identifier;

public final class Tex {
    final Identifier id;

    private Tex(Identifier id) {
        this.id = id;
    }

    public static Tex of(String path) {
        return new Tex(Identifier.withDefaultNamespace(path));
    }
}
