package com.mahghuuuls.jass.api;

import net.minecraft.util.ResourceLocation;

/** A provider with its namespaced id, as registered. */
public final class RegisteredProvider<T> {

    private final ResourceLocation id;
    private final T provider;

    RegisteredProvider(ResourceLocation id, T provider) {
        this.id = id;
        this.provider = provider;
    }

    public ResourceLocation getId() {
        return id;
    }

    public T getProvider() {
        return provider;
    }
}
