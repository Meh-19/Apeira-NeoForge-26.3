package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy ORForged mod's stone variants (its "Earth" blocks) and stained stone, each a plain block copying the
 * vanilla block ORForged copied, with a block item of the same name.
 *
 * <p>ORForged registered them without textures; theirs are the Origin Realms server pack's
 * ({@code assets/originrealms/textures/block/misc_blocks/earth/} and {@code .../stone/}). Chiseled blocks and slate
 * have a {@code block/<name>_top} end texture as well as their {@code block/<name>} side, like chiseled quartz.
 */
public final class MainStones {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredBlock<Block>> ALL = new ArrayList<>();
    private static final List<DeferredBlock<Block>> COLUMNS = new ArrayList<>();

    public static final DeferredBlock<Block> ANDESITE_BRICKS = stone("andesite_bricks", Blocks.ANDESITE);
    public static final DeferredBlock<Block> CHISELED_ANDESITE = column("chiseled_andesite", Blocks.ANDESITE);
    public static final DeferredBlock<Block> DIORITE_BRICKS = stone("diorite_bricks", Blocks.DIORITE);
    public static final DeferredBlock<Block> CHISELED_DIORITE = column("chiseled_diorite", Blocks.DIORITE);
    public static final DeferredBlock<Block> GRANITE_BRICKS = stone("granite_bricks", Blocks.GRANITE);
    public static final DeferredBlock<Block> CHISELED_GRANITE = column("chiseled_granite", Blocks.GRANITE);
    public static final DeferredBlock<Block> CHISELED_STONE = column("chiseled_stone", Blocks.STONE);
    public static final DeferredBlock<Block> DRIPSTONE_BRICKS = stone("dripstone_bricks", Blocks.DRIPSTONE_BLOCK);
    public static final DeferredBlock<Block> CHISELED_DRIPSTONE = column("chiseled_dripstone", Blocks.DRIPSTONE_BLOCK);
    public static final DeferredBlock<Block> POLISHED_DRIPSTONE = stone("polished_dripstone", Blocks.DRIPSTONE_BLOCK);
    public static final DeferredBlock<Block> CHISELED_MUD = column("chiseled_mud", Blocks.STONE);
    public static final DeferredBlock<Block> POLISHED_MUD = stone("polished_mud", Blocks.STONE);
    public static final DeferredBlock<Block> SLATE = column("slate", Blocks.STONE);
    public static final DeferredBlock<Block> SLATE_BRICKS = stone("slate_bricks", Blocks.STONE);
    public static final DeferredBlock<Block> CHISELED_SLATE = column("chiseled_slate", Blocks.STONE);
    public static final DeferredBlock<Block> POLISHED_SLATE = stone("polished_slate", Blocks.STONE);
    public static final DeferredBlock<Block> LIMESTONE = stone("limestone", Blocks.STONE);
    public static final DeferredBlock<Block> LIMESTONE_BRICKS = stone("limestone_bricks", Blocks.STONE);
    public static final DeferredBlock<Block> SANDY_LIMESTONE_BRICKS = stone("sandy_limestone_bricks", Blocks.STONE);
    public static final DeferredBlock<Block> CHISELED_LIMESTONE = column("chiseled_limestone", Blocks.STONE);
    public static final DeferredBlock<Block> POLISHED_LIMESTONE = stone("polished_limestone", Blocks.STONE);
    public static final DeferredBlock<Block> PACKED_DIRT = stone("packed_dirt", Blocks.DIRT);
    public static final DeferredBlock<Block> PACKED_SAND = stone("packed_sand", Blocks.SAND);
    public static final DeferredBlock<Block> PACKED_RED_SAND = stone("packed_red_sand", Blocks.RED_SAND);
    public static final DeferredBlock<Block> SILT = stone("silt", Blocks.DIRT);
    public static final DeferredBlock<Block> DRIED_SILT = stone("dried_silt", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> SILT_BRICKS = stone("silt_bricks", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_SILT_BRICKS = column("chiseled_silt_bricks", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> POLISHED_SILT = stone("polished_silt", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> BLUE_NETHER_BRICKS = stone("blue_nether_bricks", Blocks.NETHER_BRICKS);
    public static final DeferredBlock<Block> CHISELED_BLUE_NETHER_BRICKS = column("chiseled_blue_nether_bricks", Blocks.NETHER_BRICKS);
    public static final DeferredBlock<Block> POLISHED_BLUE_NETHER_BRICKS = stone("polished_blue_nether_bricks", Blocks.NETHER_BRICKS);
    public static final DeferredBlock<Block> TAINTED_NETHER_BRICKS = stone("tainted_nether_bricks", Blocks.NETHER_BRICKS);
    public static final DeferredBlock<Block> CHISELED_RED_NETHER_BRICKS = column("chiseled_red_nether_bricks", Blocks.NETHER_BRICKS);
    public static final DeferredBlock<Block> POLISHED_RED_NETHER_BRICKS = stone("polished_red_nether_bricks", Blocks.NETHER_BRICKS);
    public static final DeferredBlock<Block> SOUL_BRICKS = stone("soul_bricks", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> POLISHED_SOUL_SAND = stone("polished_soul_sand", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_SOUL_BRICKS = column("chiseled_soul_bricks", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> POSSESSED_CHISELED_SOUL_BRICKS = column("possessed_chiseled_soul_bricks", Blocks.STONE_BRICKS);
    public static final DeferredBlock<Block> POLISHED_BASALT_BRICKS = stone("polished_basalt_bricks", Blocks.POLISHED_BASALT);
    public static final DeferredBlock<Block> POLISHED_BASALT_TILES = stone("polished_basalt_tiles", Blocks.POLISHED_BASALT);
    public static final DeferredBlock<Block> CUT_POLISHED_BASALT = stone("cut_polished_basalt", Blocks.POLISHED_BASALT);
    public static final DeferredBlock<Block> CHISELED_POLISHED_BASALT = column("chiseled_polished_basalt", Blocks.POLISHED_BASALT);

    /** Stone in seven darker and two lighter shades; ORForged named them all "Stone". */
    public static final List<DeferredBlock<Block>> STAINED_STONE = List.of(stone("stained_stone_light_15", Blocks.STONE),
            stone("stained_stone_light_30", Blocks.STONE), stone("stained_stone_dark_15", Blocks.STONE),
            stone("stained_stone_dark_30", Blocks.STONE), stone("stained_stone_dark_45", Blocks.STONE),
            stone("stained_stone_dark_60", Blocks.STONE), stone("stained_stone_dark_75", Blocks.STONE),
            stone("stained_stone_dark_90", Blocks.STONE), stone("stained_stone_dark_105", Blocks.STONE));

    private MainStones() {}

    /** Every block registered here, in registration order. */
    public static List<DeferredBlock<Block>> all() {
        return List.copyOf(ALL);
    }

    /** The blocks with an end texture of their own: the chiseled blocks and slate. */
    public static List<DeferredBlock<Block>> columns() {
        return List.copyOf(COLUMNS);
    }

    /** The blocks dug with a shovel, as ORForged tagged them: packed dirt and sands, and silt. */
    public static List<DeferredBlock<Block>> soils() {
        return List.of(PACKED_DIRT, PACKED_SAND, PACKED_RED_SAND, SILT);
    }

    private static DeferredBlock<Block> stone(String name, Block like) {
        DeferredBlock<Block> block = R.blockWithItem(name, Block::new, () -> BlockBehaviour.Properties.ofLegacyCopy(like), p -> p);
        ALL.add(block);
        return block;
    }

    private static DeferredBlock<Block> column(String name, Block like) {
        DeferredBlock<Block> block = stone(name, like);
        COLUMNS.add(block);
        return block;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
