package com.wrapped.compat;

import net.minecraft.resources.ResourceLocation;

public final class Tex {
    final ResourceLocation id;

    private Tex(ResourceLocation id) {
        this.id = id;
    }

    public static Tex of(String path) {
        return new Tex(ResourceLocation.withDefaultNamespace(path));
    }
}
