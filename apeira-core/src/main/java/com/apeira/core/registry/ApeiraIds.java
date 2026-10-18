package com.apeira.core.registry;

import net.minecraft.resources.Identifier;

/**
 * Identifier helpers for the shared {@code apeira} namespace.
 *
 * <p>Minecraft 26.x renamed {@code ResourceLocation} to {@link Identifier}; {@link Identifier#fromNamespaceAndPath}
 * is the direct replacement for {@code ResourceLocation.fromNamespaceAndPath}.
 */
public final class ApeiraIds {
    /** Namespace shared by every Apeira module's registry entries, data and assets. */
    public static final String NAMESPACE = "apeira";

    private ApeiraIds() {}

    /** Returns {@code apeira:<path>}. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    /** Returns {@code <namespace>:<path>}, for modules that register under their own namespace. */
    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
