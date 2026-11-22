package com.apeira.decor.furniture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.multiblock.Footprint;

import net.minecraft.world.level.block.Block;

/**
 * Live {@link FurnitureTuning}s of furniture pieces, by block, which {@link FurnitureBlock} uses in place of its code
 * values (and its baked {@link FurnitureTunes}). A dev tool (apeira-dev) {@linkplain #enable() switches them on} from its
 * mod constructor and keeps both sides' tunings in step; without it they are off.
 *
 * <p>One map serves both sides: in singleplayer the client and the integrated server set the same values.
 */
public final class FurnitureTunings {
    private static volatile boolean enabled;
    private static final Map<Block, Entry> TUNINGS = new ConcurrentHashMap<>();

    private FurnitureTunings() {}

    /** Lets furniture be tuned. Call from a mod constructor. */
    public static void enable() {
        enabled = true;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** {@code block}'s live tuning, or {@code null} while it has its code values. */
    public static @Nullable FurnitureTuning get(Block block) {
        if (!enabled) {
            return null;
        }
        Entry entry = TUNINGS.get(block);
        return entry != null ? entry.tuning : null;
    }

    /** {@code block}'s footprint cut down by its live tuning, worked out once per tuning from {@code base}. */
    static @Nullable Footprint footprint(Block block, Footprint base) {
        if (!enabled) {
            return null;
        }
        Entry entry = TUNINGS.get(block);
        if (entry == null) {
            return null;
        }
        Footprint known = entry.footprint;
        if (known == null) {
            FurnitureTuning tuning = entry.tuning;
            known = base.without(tuning::removes);
            entry.footprint = known;
        }
        return known;
    }

    /** Tunes {@code block}, or gives it back its code values with {@code null}. */
    public static void set(Block block, @Nullable FurnitureTuning tuning) {
        if (tuning == null) {
            TUNINGS.remove(block);
        } else {
            TUNINGS.put(block, new Entry(tuning));
        }
    }

    /** Every tuned piece. */
    public static Map<Block, FurnitureTuning> all() {
        Map<Block, FurnitureTuning> all = new LinkedHashMap<>();
        TUNINGS.forEach((block, entry) -> all.put(block, entry.tuning));
        return all;
    }

    public static void clear() {
        TUNINGS.clear();
    }

    private static final class Entry {
        final FurnitureTuning tuning;
        volatile @Nullable Footprint footprint;

        Entry(FurnitureTuning tuning) {
            this.tuning = tuning;
        }
    }
}
