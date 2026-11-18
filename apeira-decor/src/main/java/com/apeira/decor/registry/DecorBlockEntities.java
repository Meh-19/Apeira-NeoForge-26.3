package com.apeira.decor.registry;

import java.util.HashSet;
import java.util.Set;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.furniture.AnimatedFurnitureBlock;
import com.apeira.decor.furniture.AnimatedFurnitureBlockEntity;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlockEntity;
import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block entities of the decor module. */
public final class DecorBlockEntities {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /**
     * {@code apeira:paint}: the paint of a paintable block that has no block entity of its own. Add every such
     * {@code PaintableBlock} here; there are none yet, since furniture keeps its paint in its own block entities.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintBlockEntity>> PAINT = R.blockEntityTypes().register(
            "paint", () -> new BlockEntityType<>(PaintBlockEntity::new, Set.of()));

    /**
     * Every furniture piece's paint, drawn by {@code FurnitureRenderer}: valid for every {@link FurnitureBlock} of any
     * module but the animated ones, collected from the block registry, which is filled before block entity types are
     * registered. Its id is {@code apeira:artifact_display}, archaeology's from before furniture became one system, so
     * placed artifacts keep their paint.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnitureBlockEntity>> FURNITURE = R.blockEntityTypes().register(
            "artifact_display", () -> new BlockEntityType<>(FurnitureBlockEntity::new, furniture(false)));

    /** An animated piece's paint and GeckoLib animation ({@code apeira:generic_animated_furniture}, archaeology's id too). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnimatedFurnitureBlockEntity>> ANIMATED_FURNITURE = R.blockEntityTypes()
            .register("generic_animated_furniture", () -> new BlockEntityType<>(AnimatedFurnitureBlockEntity::new, furniture(true)));

    private DecorBlockEntities() {}

    private static Set<Block> furniture(boolean animated) {
        Set<Block> blocks = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof FurnitureBlock && block instanceof AnimatedFurnitureBlock == animated) {
                blocks.add(block);
            }
        }
        return Set.copyOf(blocks);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
