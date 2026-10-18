package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.core.registry.ShapeSet;
import com.apeira.main.Apeira;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy Xenon mod's stones (its {@code ModBlockFamilies.STONES}), with Xenon's ids and properties
 * ({@link MainBlockProperties}): new stones with their stairs, slabs and walls ({@link ShapeSet}), shapes for the
 * ORForged stones of {@link MainStones} that Xenon gave them, and ashen sand, loam and permafrost.
 *
 * <p>Textures are Xenon's, renamed to Apeira's convention: {@code block/<name>}, with {@code block/<name>_top} for
 * the ends of pillars and chiseled blocks (Xenon's {@code <name>_side} is {@code <name>}). Shapes use their base
 * block's texture.
 */
public final class MainRocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();
    private static final List<DeferredBlock<? extends Block>> COLUMNS = new ArrayList<>();
    private static final List<DeferredBlock<? extends Block>> PILLARS = new ArrayList<>();
    private static final List<ShapeSet> SHAPES = new ArrayList<>();
    /** The colour of the dust falling from ashen sand: the average of its texture. */
    private static final int ASHEN_SAND_DUST = 0xFFA28982;

    // Ashen sand and sandstone. Sandstones have their own top and bottom textures.
    public static final DeferredBlock<ColoredFallingBlock> ASHEN_SAND = block("ashen_sand",
            p -> new ColoredFallingBlock(new ColorRGBA(ASHEN_SAND_DUST), p), MainBlockProperties::sand);
    public static final DeferredBlock<Block> PACKED_ASHEN_SAND = block("packed_ashen_sand", Block::new, MainBlockProperties::packedSand);
    public static final DeferredBlock<Block> ASHEN_SANDSTONE = stone("ashen_sandstone");
    public static final DeferredBlock<Block> SMOOTH_ASHEN_SANDSTONE = stone("smooth_ashen_sandstone");
    public static final DeferredBlock<Block> CHISELED_ASHEN_SANDSTONE = stone("chiseled_ashen_sandstone");
    public static final DeferredBlock<Block> CUT_ASHEN_SANDSTONE = stone("cut_ashen_sandstone");

    // Loam and permafrost, dug with a shovel.
    public static final DeferredBlock<Block> LOAM = stone("loam");
    public static final DeferredBlock<Block> PERMAFROST = stone("permafrost");

    // Shapes for ORForged's limestone.
    public static final ShapeSet LIMESTONE_SHAPES = shapes(MainStones.LIMESTONE);
    public static final ShapeSet POLISHED_LIMESTONE_SHAPES = shapes(MainStones.POLISHED_LIMESTONE);
    public static final ShapeSet LIMESTONE_BRICKS_SHAPES = shapes(MainStones.LIMESTONE_BRICKS);
    public static final ShapeSet SANDY_LIMESTONE_BRICKS_SHAPES = shapes(MainStones.SANDY_LIMESTONE_BRICKS);

    // Rhyolite.
    public static final DeferredBlock<Block> RHYOLITE = stone("rhyolite");
    public static final ShapeSet RHYOLITE_SHAPES = shapes(RHYOLITE);
    public static final DeferredBlock<Block> MOSSY_RHYOLITE = stone("mossy_rhyolite");
    public static final ShapeSet MOSSY_RHYOLITE_SHAPES = shapes(MOSSY_RHYOLITE);
    public static final DeferredBlock<RotatedPillarBlock> RHYOLITE_PILLAR = pillar("rhyolite_pillar");
    public static final DeferredBlock<Block> RHYOLITE_BRICKS = stone("rhyolite_bricks");
    public static final ShapeSet RHYOLITE_BRICKS_SHAPES = shapes(RHYOLITE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_RHYOLITE = column("chiseled_rhyolite");
    public static final DeferredBlock<Block> CRACKED_RHYOLITE_BRICKS = stone("cracked_rhyolite_bricks");
    public static final ShapeSet CRACKED_RHYOLITE_BRICKS_SHAPES = shapes(CRACKED_RHYOLITE_BRICKS);
    public static final DeferredBlock<Block> MOSSY_RHYOLITE_BRICKS = stone("mossy_rhyolite_bricks");
    public static final ShapeSet MOSSY_RHYOLITE_BRICKS_SHAPES = shapes(MOSSY_RHYOLITE_BRICKS);

    // Kyanite.
    public static final DeferredBlock<Block> KYANITE = stone("kyanite");
    public static final ShapeSet KYANITE_SHAPES = shapes(KYANITE);
    public static final DeferredBlock<Block> POLISHED_KYANITE = stone("polished_kyanite");
    public static final ShapeSet POLISHED_KYANITE_SHAPES = shapes(POLISHED_KYANITE);
    public static final DeferredBlock<Block> KYANITE_BRICKS = stone("kyanite_bricks");
    public static final ShapeSet KYANITE_BRICKS_SHAPES = shapes(KYANITE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_KYANITE = column("chiseled_kyanite");

    // Grimrock.
    public static final DeferredBlock<Block> GRIMROCK = stone("grimrock");
    public static final ShapeSet GRIMROCK_SHAPES = shapes(GRIMROCK);
    public static final DeferredBlock<Block> POLISHED_GRIMROCK = stone("polished_grimrock");
    public static final ShapeSet POLISHED_GRIMROCK_SHAPES = shapes(POLISHED_GRIMROCK);
    public static final DeferredBlock<Block> GRIMROCK_BRICKS = stone("grimrock_bricks");
    public static final ShapeSet GRIMROCK_BRICKS_SHAPES = shapes(GRIMROCK_BRICKS);
    public static final DeferredBlock<Block> CHISELED_GRIMROCK = column("chiseled_grimrock");
    public static final DeferredBlock<Block> CRACKED_GRIMROCK_BRICKS = stone("cracked_grimrock_bricks");
    public static final ShapeSet CRACKED_GRIMROCK_BRICKS_SHAPES = shapes(CRACKED_GRIMROCK_BRICKS);
    public static final DeferredBlock<Block> MOSSY_GRIMROCK_BRICKS = stone("mossy_grimrock_bricks");
    public static final ShapeSet MOSSY_GRIMROCK_BRICKS_SHAPES = shapes(MOSSY_GRIMROCK_BRICKS);
    public static final DeferredBlock<Block> COBBLED_GRIMROCK = stone("cobbled_grimrock");
    public static final ShapeSet COBBLED_GRIMROCK_SHAPES = shapes(COBBLED_GRIMROCK);

    // Packed loam.
    public static final DeferredBlock<Block> PACKED_LOAM = stone("packed_loam");
    public static final ShapeSet PACKED_LOAM_SHAPES = shapes(PACKED_LOAM);
    public static final DeferredBlock<Block> PACKED_LOAM_BRICKS = stone("packed_loam_bricks");
    public static final ShapeSet PACKED_LOAM_BRICKS_SHAPES = shapes(PACKED_LOAM_BRICKS);
    public static final DeferredBlock<Block> CHISELED_PACKED_LOAM = column("chiseled_packed_loam");
    public static final DeferredBlock<Block> MOSSY_PACKED_LOAM_BRICKS = stone("mossy_packed_loam_bricks");
    public static final ShapeSet MOSSY_PACKED_LOAM_BRICKS_SHAPES = shapes(MOSSY_PACKED_LOAM_BRICKS);

    // Shapes for ORForged's silt and slate, and a slab for its packed dirt.
    public static final ShapeSet DRIED_SILT_SHAPES = shapes(MainStones.DRIED_SILT);
    public static final ShapeSet POLISHED_SILT_SHAPES = shapes(MainStones.POLISHED_SILT);
    public static final ShapeSet SILT_BRICKS_SHAPES = shapes(MainStones.SILT_BRICKS);
    public static final ShapeSet SLATE_SHAPES = shapes(MainStones.SLATE);
    public static final ShapeSet POLISHED_SLATE_SHAPES = shapes(MainStones.POLISHED_SLATE);
    public static final ShapeSet SLATE_BRICKS_SHAPES = shapes(MainStones.SLATE_BRICKS);
    public static final ShapeSet PACKED_DIRT_SHAPES = shapes(MainStones.PACKED_DIRT, MainBlockProperties::packedDirt, Set.of(ShapeSet.Shape.SLAB));

    // Strata.
    public static final DeferredBlock<Block> STRATA = stone("strata");
    public static final ShapeSet STRATA_SHAPES = shapes(STRATA);
    public static final DeferredBlock<Block> POLISHED_STRATA = stone("polished_strata");
    public static final ShapeSet POLISHED_STRATA_SHAPES = shapes(POLISHED_STRATA);
    public static final DeferredBlock<Block> STRATA_BRICKS = stone("strata_bricks");
    public static final ShapeSet STRATA_BRICKS_SHAPES = shapes(STRATA_BRICKS);
    public static final DeferredBlock<Block> CHISELED_STRATA = column("chiseled_strata");

    // Dolomite. Porous dolomite pushes bubble columns up, like soul sand (it is in #minecraft:enables_bubble_column_push_up).
    public static final DeferredBlock<Block> DOLOMITE = stone("dolomite");
    public static final ShapeSet DOLOMITE_SHAPES = shapes(DOLOMITE);
    public static final DeferredBlock<RotatedPillarBlock> DOLOMITE_PILLAR = pillar("dolomite_pillar");
    public static final DeferredBlock<Block> POLISHED_DOLOMITE = stone("polished_dolomite");
    public static final ShapeSet POLISHED_DOLOMITE_SHAPES = shapes(POLISHED_DOLOMITE);
    public static final DeferredBlock<Block> DOLOMITE_BRICKS = stone("dolomite_bricks");
    public static final ShapeSet DOLOMITE_BRICKS_SHAPES = shapes(DOLOMITE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_DOLOMITE = column("chiseled_dolomite");
    public static final DeferredBlock<Block> POROUS_DOLOMITE = stone("porous_dolomite");

    // Gravestone.
    public static final DeferredBlock<Block> GRAVESTONE = stone("gravestone");
    public static final ShapeSet GRAVESTONE_SHAPES = shapes(GRAVESTONE);
    public static final DeferredBlock<Block> POLISHED_GRAVESTONE = stone("polished_gravestone");
    public static final ShapeSet POLISHED_GRAVESTONE_SHAPES = shapes(POLISHED_GRAVESTONE);
    public static final DeferredBlock<Block> GRAVESTONE_BRICKS = stone("gravestone_bricks");
    public static final ShapeSet GRAVESTONE_BRICKS_SHAPES = shapes(GRAVESTONE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_GRAVESTONE = column("chiseled_gravestone");

    // Scoria and molten scoria, pillars like basalt.
    public static final DeferredBlock<RotatedPillarBlock> SCORIA = pillar("scoria");
    public static final DeferredBlock<Block> SCORIA_BRICKS = stone("scoria_bricks");
    public static final ShapeSet SCORIA_BRICKS_SHAPES = shapes(SCORIA_BRICKS);
    public static final DeferredBlock<Block> CHISELED_SCORIA = column("chiseled_scoria");
    public static final DeferredBlock<RotatedPillarBlock> MOLTEN_SCORIA = pillar("molten_scoria");
    public static final DeferredBlock<Block> CHISELED_MOLTEN_SCORIA = column("chiseled_molten_scoria");
    public static final DeferredBlock<Block> MOLTEN_SCORIA_BRICKS = stone("molten_scoria_bricks");
    public static final ShapeSet MOLTEN_SCORIA_BRICKS_SHAPES = shapes(MOLTEN_SCORIA_BRICKS);

    // Shapes for ORForged's dripstone and basalt bricks.
    public static final ShapeSet POLISHED_DRIPSTONE_SHAPES = shapes(MainStones.POLISHED_DRIPSTONE);
    public static final ShapeSet DRIPSTONE_BRICKS_SHAPES = shapes(MainStones.DRIPSTONE_BRICKS);
    public static final ShapeSet POLISHED_BASALT_BRICKS_SHAPES = shapes(MainStones.POLISHED_BASALT_BRICKS);
    public static final ShapeSet POLISHED_BASALT_TILES_SHAPES = shapes(MainStones.POLISHED_BASALT_TILES);
    public static final ShapeSet CUT_POLISHED_BASALT_SHAPES = shapes(MainStones.CUT_POLISHED_BASALT);

    // Nether bricks: shapes for ORForged's blue and tainted nether bricks, and polished nether bricks.
    public static final ShapeSet BLUE_NETHER_BRICKS_SHAPES = shapes(MainStones.BLUE_NETHER_BRICKS, MainBlockProperties::netherBrick, ShapeSet.ALL);
    public static final ShapeSet TAINTED_NETHER_BRICKS_SHAPES = shapes(MainStones.TAINTED_NETHER_BRICKS, MainBlockProperties::netherBrick, ShapeSet.ALL);
    public static final DeferredBlock<Block> POLISHED_NETHER_BRICKS = block("polished_nether_bricks", Block::new, MainBlockProperties::netherBrick);

    // Shapes for ORForged's soul blocks. Xenon called polished soul sand polished_soulsand; Apeira keeps ORForged's id.
    public static final ShapeSet POLISHED_SOUL_SAND_SHAPES = shapes(MainStones.POLISHED_SOUL_SAND);
    public static final ShapeSet SOUL_BRICKS_SHAPES = shapes(MainStones.SOUL_BRICKS);

    // Packed snow and snow bricks.
    public static final DeferredBlock<Block> PACKED_SNOW = stone("packed_snow");
    public static final ShapeSet PACKED_SNOW_SHAPES = shapes(PACKED_SNOW);
    public static final DeferredBlock<Block> SNOW_BRICKS = stone("snow_bricks");
    public static final ShapeSet SNOW_BRICKS_SHAPES = shapes(SNOW_BRICKS);
    public static final DeferredBlock<Block> CHISELED_SNOW_BRICKS = column("chiseled_snow_bricks");

    // Packed ice.
    public static final DeferredBlock<Block> POLISHED_PACKED_ICE = stone("polished_packed_ice");
    public static final ShapeSet POLISHED_PACKED_ICE_SHAPES = shapes(POLISHED_PACKED_ICE);
    public static final DeferredBlock<Block> PACKED_ICE_BRICKS = stone("packed_ice_bricks");
    public static final ShapeSet PACKED_ICE_BRICKS_SHAPES = shapes(PACKED_ICE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_PACKED_ICE_BRICKS = column("chiseled_packed_ice_bricks");

    // Prismarine.
    public static final DeferredBlock<RotatedPillarBlock> PRISMARINE_PILLAR = pillar("prismarine_pillar");
    public static final DeferredBlock<Block> POLISHED_PRISMARINE = stone("polished_prismarine");
    public static final ShapeSet POLISHED_PRISMARINE_SHAPES = shapes(POLISHED_PRISMARINE);
    public static final DeferredBlock<Block> PRISMARINE_TILES = stone("prismarine_tiles");
    public static final ShapeSet PRISMARINE_TILES_SHAPES = shapes(PRISMARINE_TILES);
    public static final DeferredBlock<Block> CHISELED_PRISMARINE = column("chiseled_prismarine");

    private MainRocks() {}

    /** Every block registered here, shapes included, in registration order. */
    public static List<DeferredBlock<? extends Block>> all() {
        return List.copyOf(ALL);
    }

    /** The chiseled blocks, with an end texture of their own. */
    public static List<DeferredBlock<? extends Block>> columns() {
        return List.copyOf(COLUMNS);
    }

    /** The pillars, which turn with their axis like logs. */
    public static List<DeferredBlock<? extends Block>> pillars() {
        return List.copyOf(PILLARS);
    }

    /** Every set of stairs, slab and wall, on new stones and on ORForged's. */
    public static List<ShapeSet> shapes() {
        return List.copyOf(SHAPES);
    }

    /** The blocks dug with a shovel, as Xenon tagged them: ashen sand, packed ashen sand, loam and permafrost. */
    public static List<DeferredBlock<? extends Block>> soils() {
        return List.of(ASHEN_SAND, PACKED_ASHEN_SAND, LOAM, PERMAFROST);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = R.blockWithItem(name, factory, properties, p -> p);
        ALL.add(block);
        return block;
    }

    private static DeferredBlock<Block> stone(String name) {
        return block(name, Block::new, MainBlockProperties::stone);
    }

    private static DeferredBlock<Block> column(String name) {
        DeferredBlock<Block> block = stone(name);
        COLUMNS.add(block);
        return block;
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(String name) {
        DeferredBlock<RotatedPillarBlock> block = block(name, RotatedPillarBlock::new, MainBlockProperties::stone);
        PILLARS.add(block);
        return block;
    }

    private static ShapeSet shapes(DeferredBlock<? extends Block> base) {
        return shapes(base, MainBlockProperties::stone, ShapeSet.ALL);
    }

    private static ShapeSet shapes(DeferredBlock<? extends Block> base, Supplier<BlockBehaviour.Properties> properties, Set<ShapeSet.Shape> shapes) {
        ShapeSet set = ShapeSet.register(R, base, base.getId().getPath(), properties, shapes);
        ALL.addAll(set.shapes());
        SHAPES.add(set);
        return set;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
