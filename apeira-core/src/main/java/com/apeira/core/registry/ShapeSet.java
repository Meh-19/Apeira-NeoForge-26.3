package com.apeira.core.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.data.BlockFamily;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The stairs, slab and wall made from a building block, as vanilla has for stone bricks: {@code <name>_stairs},
 * {@code _slab} and {@code _wall}, each with a block item, any of which a set may lack. {@link #family()} gives the
 * vanilla {@link BlockFamily} that datagen uses for their models (textured with the base block's
 * {@code block/<base>}) and their crafting recipes.
 */
public record ShapeSet(DeferredBlock<? extends Block> base, @Nullable DeferredBlock<StairBlock> stairs,
        @Nullable DeferredBlock<SlabBlock> slab, @Nullable DeferredBlock<WallBlock> wall) {
    public enum Shape {
        STAIRS, SLAB, WALL
    }

    /** Every shape. */
    public static final Set<Shape> ALL = Set.of(Shape.STAIRS, Shape.SLAB, Shape.WALL);
    /** Stairs and a slab, without a wall. */
    public static final Set<Shape> STAIRS_AND_SLAB = Set.of(Shape.STAIRS, Shape.SLAB);

    /** Registers the {@code shapes} of {@code base}, named {@code <name>_<shape>}, with {@code properties}. */
    public static ShapeSet register(ApeiraRegistrar registrar, DeferredBlock<? extends Block> base, String name,
            Supplier<BlockBehaviour.Properties> properties, Set<Shape> shapes) {
        return new ShapeSet(base,
                shapes.contains(Shape.STAIRS)
                        ? registrar.blockWithItem(name + "_stairs", p -> new StairBlock(base.get().defaultBlockState(), p), properties, p -> p)
                        : null,
                shapes.contains(Shape.SLAB) ? registrar.blockWithItem(name + "_slab", SlabBlock::new, properties, p -> p) : null,
                shapes.contains(Shape.WALL) ? registrar.blockWithItem(name + "_wall", WallBlock::new, properties, p -> p) : null);
    }

    /** The shapes the set has, stairs first. */
    public List<DeferredBlock<? extends Block>> shapes() {
        List<DeferredBlock<? extends Block>> shapes = new ArrayList<>(3);
        if (stairs != null) {
            shapes.add(stairs);
        }
        if (slab != null) {
            shapes.add(slab);
        }
        if (wall != null) {
            shapes.add(wall);
        }
        return shapes;
    }

    /** The vanilla block family of the set, for model and recipe generation. Only call once blocks are registered. */
    public BlockFamily family() {
        BlockFamily.Builder family = new BlockFamily.Builder(base.get());
        if (stairs != null) {
            family.stairs(stairs.get());
        }
        if (slab != null) {
            family.slab(slab.get());
        }
        if (wall != null) {
            family.wall(wall.get());
        }
        return family.getFamily();
    }
}
