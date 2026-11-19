package com.apeira.decor.furniture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.apeira.decor.multiblock.Footprint;
import com.apeira.decor.multiblock.FootprintTable;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Every furniture piece's multiblock {@link Footprint}, by block name, from the footprint tables of the modules that
 * have furniture: decor's {@code apeira_decor/furniture_footprints.txt} and whatever others add ({@link #addTable};
 * apeira-archae adds {@code apeira_archae/artifact_footprints.txt}). The tables are cut from the pieces' models as they
 * are drawn, so a footprint may reach past its block and outlines the model's own edges. A piece in no table uses its
 * {@link FurnitureBlock.Form}'s shape as its footprint.
 */
public final class FurnitureFootprints {
    private static final List<FootprintTable> TABLES = new CopyOnWriteArrayList<>(
            List.of(FootprintTable.read(FurnitureFootprints.class, "/apeira_decor/furniture_footprints.txt")));

    private FurnitureFootprints() {}

    /**
     * Adds a module's table, {@code resource} in the module of {@code owner} (a class of that mod: a decor class cannot
     * see another mod's resources); call it before the module's furniture registers.
     */
    public static void addTable(Class<?> owner, String resource) {
        TABLES.add(FootprintTable.read(owner, resource));
    }

    /**
     * A piece's block properties with what every piece needs: shapes cut from the footprint when a turn is first asked
     * for ({@code dynamicShape}), not for every state at load; and a piece covering more than its own block cannot be
     * pushed by pistons, which would leave its parts behind.
     */
    public static BlockBehaviour.Properties properties(String name, BlockBehaviour.Properties properties) {
        BlockBehaviour.Properties dynamic = properties.dynamicShape();
        return of(name, Shapes.block()).single() ? dynamic : dynamic.pushReaction(PushReaction.IMMOVEABLE);
    }

    public static Footprint of(String name, VoxelShape fallback) {
        for (FootprintTable table : TABLES) {
            var footprint = table.get(name);
            if (footprint.isPresent()) {
                return footprint.get();
            }
        }
        List<AABB> boxes = fallback.toAabbs();
        return boxes.isEmpty() ? Footprint.BLOCK : Footprint.of(boxes);
    }
}
