package com.apeira.decor.furniture;

import com.apeira.decor.multiblock.Footprint;
import com.apeira.decor.multiblock.FootprintTable;

import net.minecraft.world.phys.AABB;

/**
 * Each furniture piece's multiblock {@link Footprint}: from {@code apeira_decor/furniture_footprints.txt} (cut from the
 * piece's model as it is drawn, so it can reach past its block and outlines the model's own edges, as artifacts do), or
 * for a piece not in the table, its catalogue shape as a one-block footprint.
 */
public final class FurnitureFootprints {
    private static final FootprintTable TABLE = FootprintTable.read("/apeira_decor/furniture_footprints.txt");

    private FurnitureFootprints() {}

    public static Footprint of(Furniture furniture) {
        return TABLE.get(furniture.name()).orElseGet(() -> {
            java.util.List<AABB> boxes = furniture.shape().toAabbs();
            return boxes.isEmpty() ? Footprint.BLOCK : Footprint.of(boxes);
        });
    }
}
