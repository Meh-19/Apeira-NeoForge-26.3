package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.core.registry.ShapeSet;
import com.apeira.main.Apeira;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy Xenon mod's coloured building blocks, one of each per dye colour, in Xenon's colour order: concrete
 * bricks ({@code concrete_bricks_<colour>}, with stairs and a slab), terracotta shingles ({@code terracotta_shingles}
 * and {@code terracotta_shingles_<colour>}, with stairs and a slab) and quilted wool ({@code quilted_wool_<colour>}).
 * Each is a cube with the texture {@code block/<name>}; their properties are Xenon's ({@link MainBlockProperties}).
 */
public final class MainDyedBlocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /** Xenon's colour order. */
    public static final List<DyeColor> COLOURS = List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
            DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN,
            DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK);

    /** A coloured block and its shapes, which some blocks lack. */
    public record Dyed(DyeColor colour, DeferredBlock<Block> block, ShapeSet shapes) {}

    public static final List<Dyed> CONCRETE_BRICKS = colours("concrete_bricks", MainBlockProperties::stone, true);
    /** The undyed shingles, the colour of plain terracotta. */
    public static final DeferredBlock<Block> TERRACOTTA_SHINGLES = block("terracotta_shingles", MainBlockProperties::terracottaShingles);
    public static final ShapeSet TERRACOTTA_SHINGLES_SHAPES = shapes(TERRACOTTA_SHINGLES, MainBlockProperties::terracottaShingles);
    public static final List<Dyed> TERRACOTTA_SHINGLES_COLOURS = colours("terracotta_shingles", MainBlockProperties::terracottaShingles, true);
    public static final List<Dyed> QUILTED_WOOL = colours("quilted_wool", MainBlockProperties::quiltedWool, false);

    private MainDyedBlocks() {}

    /** Every block registered here, shapes included. */
    public static List<DeferredBlock<? extends Block>> all() {
        List<DeferredBlock<? extends Block>> all = new ArrayList<>();
        for (Dyed dyed : CONCRETE_BRICKS) {
            all.add(dyed.block());
            all.addAll(dyed.shapes().shapes());
        }
        all.add(TERRACOTTA_SHINGLES);
        all.addAll(TERRACOTTA_SHINGLES_SHAPES.shapes());
        for (Dyed dyed : TERRACOTTA_SHINGLES_COLOURS) {
            all.add(dyed.block());
            all.addAll(dyed.shapes().shapes());
        }
        QUILTED_WOOL.forEach(dyed -> all.add(dyed.block()));
        return all;
    }

    /** Every set of stairs and slab: the concrete bricks' and the shingles'. */
    public static List<ShapeSet> shapes() {
        List<ShapeSet> shapes = new ArrayList<>();
        CONCRETE_BRICKS.forEach(dyed -> shapes.add(dyed.shapes()));
        shapes.add(TERRACOTTA_SHINGLES_SHAPES);
        TERRACOTTA_SHINGLES_COLOURS.forEach(dyed -> shapes.add(dyed.shapes()));
        return shapes;
    }

    /** Registers {@code <prefix>_<colour>} for every colour, each with stairs and a slab if {@code shaped}. */
    private static List<Dyed> colours(String prefix, Supplier<BlockBehaviour.Properties> properties, boolean shaped) {
        List<Dyed> blocks = new ArrayList<>();
        for (DyeColor colour : COLOURS) {
            DeferredBlock<Block> block = block(prefix + "_" + colour.getSerializedName(), properties);
            blocks.add(new Dyed(colour, block, shaped ? shapes(block, properties) : new ShapeSet(block, null, null, null)));
        }
        return List.copyOf(blocks);
    }

    private static DeferredBlock<Block> block(String name, Supplier<BlockBehaviour.Properties> properties) {
        return R.blockWithItem(name, Block::new, properties, p -> p);
    }

    private static ShapeSet shapes(DeferredBlock<Block> base, Supplier<BlockBehaviour.Properties> properties) {
        return ShapeSet.register(R, base, base.getId().getPath(), properties, ShapeSet.STAIRS_AND_SLAB);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
