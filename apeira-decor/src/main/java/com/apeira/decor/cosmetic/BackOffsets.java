package com.apeira.decor.cosmetic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.Item;

/**
 * Live {@link BackOffset}s of back cosmetics, by item, which {@code client/CosmeticLayer} adds to its back transform. Only
 * a dev tool (apeira-dev) sets them, keeping both sides in step; without it the map stays empty and every back cosmetic
 * uses the fixed transform. One map serves both sides, as {@code FurnitureTunings} does.
 */
public final class BackOffsets {
    private static final Map<Item, BackOffset> OFFSETS = new ConcurrentHashMap<>();

    private BackOffsets() {}

    /** {@code item}'s offset, or {@code null} while it has none. */
    public static @Nullable BackOffset get(Item item) {
        return OFFSETS.isEmpty() ? null : OFFSETS.get(item);
    }

    /** Sets {@code item}'s offset, or removes it with {@code null}. */
    public static void set(Item item, @Nullable BackOffset offset) {
        if (offset == null) {
            OFFSETS.remove(item);
        } else {
            OFFSETS.put(item, offset);
        }
    }

    public static Map<Item, BackOffset> all() {
        return new LinkedHashMap<>(OFFSETS);
    }

    public static void clear() {
        OFFSETS.clear();
    }
}
