package com.apeira.decor.furniture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.ApeiraFurnitureBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Live {@link FurnitureTuning}s of furniture pieces, by block, which {@link FurnitureBlock} and {@code FurnitureRenderer}
 * use in place of the catalogue's values. A dev tool (apeira-dev) {@linkplain #enable() switches them on} from its mod
 * constructor and keeps both sides' tunings in step; without it they are off, and furniture keeps its cached shapes.
 *
 * <p>One map serves both sides: in singleplayer the client and the integrated server set the same values.
 */
public final class FurnitureTunings {
    private static volatile boolean enabled;
    private static final Map<Block, Entry> TUNINGS = new ConcurrentHashMap<>();

    private FurnitureTunings() {}

    /**
     * Lets furniture be tuned. Call from a mod constructor, before blocks cache their shapes: furniture then reads its
     * shape each time, so a tuned shape takes effect at once.
     */
    public static void enable() {
        enabled = true;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** {@code block}'s tuning, or {@code null} while it has the catalogue's values. */
    public static @Nullable FurnitureTuning get(Block block) {
        if (!enabled) {
            return null;
        }
        Entry entry = TUNINGS.get(block);
        return entry != null ? entry.tuning : null;
    }

    /** {@code block}'s tuned shape at {@code rotation}, or {@code null} while it has the catalogue's. */
    static @Nullable VoxelShape shape(Block block, int rotation) {
        if (!enabled) {
            return null;
        }
        Entry entry = TUNINGS.get(block);
        return entry != null ? entry.shapes[rotation] : null;
    }

    /** Tunes {@code block}, or gives it back the catalogue's values with {@code null}. */
    public static void set(Block block, @Nullable FurnitureTuning tuning) {
        if (tuning == null) {
            TUNINGS.remove(block);
        } else {
            TUNINGS.put(block, new Entry(tuning, ApeiraFurnitureBlock.rotations(tuning.shape())));
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

    private record Entry(FurnitureTuning tuning, VoxelShape[] shapes) {}
}
