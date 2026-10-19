package com.apeira.main.world;

import java.util.List;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainStones;
import com.apeira.main.world.feature.BirchTreeFeature;
import com.apeira.main.world.feature.ColdLeafLitterFeature;
import com.apeira.main.world.feature.DampMossPatchFeature;
import com.apeira.main.world.feature.FallenBirchLogFeature;
import com.apeira.main.world.feature.FallenLogFeature;
import com.apeira.main.world.feature.FirBushFeature;
import com.apeira.main.world.feature.FirTreeFeature;
import com.apeira.main.world.feature.FoothillsPondFeature;
import com.apeira.main.world.feature.LeafLitterFeature;
import com.apeira.main.world.feature.MapleBushFeature;
import com.apeira.main.world.feature.MapleTreeFeature;
import com.apeira.main.world.feature.MycenaPatchFeature;
import com.apeira.main.world.feature.OakBushFeature;
import com.apeira.main.world.feature.PodzolPatchFeature;
import com.apeira.main.world.feature.ShelfFungusClusterFeature;
import com.apeira.main.world.feature.SlateBoulderFeature;
import com.apeira.main.world.feature.StickPatchFeature;
import com.apeira.main.world.feature.StoneMoundFeature;
import com.apeira.main.world.feature.StoneShoreFeature;
import com.apeira.main.world.feature.SurfacePatchFeature;
import com.apeira.main.world.feature.UnderwaterSiltPatchFeature;
import com.apeira.main.world.feature.WaterEdgeCattailFeature;
import com.apeira.main.world.feature.WaterEdgeDiskFeature;
import com.apeira.main.world.feature.YellowBirchTreeFeature;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BlockStateProviders;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/**
 * Xenon's configured features, with its ids and values ({@code ModConfiguredFeatures}), written by datagen as
 * {@code data/apeira/worldgen/feature/}. Xenon's random patches are plain {@code simple_block} features here: 26.x has no
 * random patch, and the tries and spread are placement modifiers ({@link MainPlacedFeatures#patch}).
 */
public final class MainConfiguredFeatures {
    public static final ResourceKey<Feature> PLATINUM_ORE = key("platinum_ore");
    public static final ResourceKey<Feature> RUBY_ORE = key("ruby_ore");
    public static final ResourceKey<Feature> SLATE_SHEET = key("slate_sheet");

    public static final ResourceKey<Feature> MAPLE_TREE = key("maple_tree");
    public static final ResourceKey<Feature> MAPLE_BUSH = key("maple_bush");
    public static final ResourceKey<Feature> FIR_TREE = key("fir_tree");
    public static final ResourceKey<Feature> FIR_BUSH = key("fir_bush");
    public static final ResourceKey<Feature> BIRCH_TREE = key("birch_tree");
    public static final ResourceKey<Feature> YELLOW_BIRCH_TREE = key("yellow_birch_tree");
    public static final ResourceKey<Feature> OAK_BUSH = key("oak_bush");
    public static final ResourceKey<Feature> MAPLE_OAK_TREE = key("maple_oak_tree");
    public static final ResourceKey<Feature> COLD_MAPLE_SPRUCE = key("cold_maple_spruce");
    public static final ResourceKey<Feature> MAPLE_BIRCH_TREE = key("maple_birch_tree");

    public static final ResourceKey<Feature> CLOVER_PATCH = key("clover_patch");
    public static final ResourceKey<Feature> SHORT_GRASS_PATCH = key("short_grass_patch");
    public static final ResourceKey<Feature> FLOWER_PATCH = key("flower_patch");
    public static final ResourceKey<Feature> PUMPKIN_PATCH = key("pumpkin_patch");
    public static final ResourceKey<Feature> MUSHROOM_PATCH = key("mushroom_patch");
    public static final ResourceKey<Feature> LILY_PAD_PATCH = key("lily_pad_patch");
    public static final ResourceKey<Feature> SMALL_LILY_PATCH = key("small_lily_patch");
    public static final ResourceKey<Feature> DRY_GRASS_PATCH = key("dry_grass_patch");
    public static final ResourceKey<Feature> FERN_PATCH = key("fern_patch");
    public static final ResourceKey<Feature> DANDELION_PATCH = key("dandelion_patch");
    public static final ResourceKey<Feature> ORANGE_TULIP_PATCH = key("orange_tulip_patch");
    public static final ResourceKey<Feature> HIGHLAND_FLOWER_PATCH = key("highland_flower_patch");
    public static final ResourceKey<Feature> ALLIUM_PATCH = key("allium_patch");
    public static final ResourceKey<Feature> SUNFLOWER_PATCH = key("sunflower_patch");
    public static final ResourceKey<Feature> DENSE_PUMPKIN = key("dense_pumpkin");
    public static final ResourceKey<Feature> COLD_BERRY_BUSH = key("cold_berry_bush");
    public static final ResourceKey<Feature> DEPTHS_MUSHROOM_PATCH = key("depths_mushroom_patch");

    public static final ResourceKey<Feature> DIORITE_MOUND = key("diorite_mound");
    public static final ResourceKey<Feature> ANDESITE_MOUND = key("andesite_mound");
    public static final ResourceKey<Feature> GRANITE_MOUND = key("granite_mound");
    public static final ResourceKey<Feature> SLATE_WATER_MOUND = key("slate_water_mound");
    public static final ResourceKey<Feature> SLATE_BOULDER = key("slate_boulder");
    public static final ResourceKey<Feature> FALLEN_OAK_LOG = key("fallen_oak_log");
    public static final ResourceKey<Feature> FALLEN_BIRCH_LOG = key("fallen_birch_log");
    public static final ResourceKey<Feature> CATTAILS = key("cattails");
    public static final ResourceKey<Feature> WATER_SILT_DISK = key("water_silt_disk");
    public static final ResourceKey<Feature> SURFACE_PATCH = key("surface_patch");
    public static final ResourceKey<Feature> LEAF_LITTER_PATCH = key("leaf_litter_patch");
    public static final ResourceKey<Feature> COLD_LEAF_LITTER = key("cold_leaf_litter");
    public static final ResourceKey<Feature> SHELF_FUNGUS_CLUSTER = key("shelf_fungus_cluster");
    public static final ResourceKey<Feature> PODZOL_PATCH = key("podzol_patch");
    public static final ResourceKey<Feature> DAMP_MOSS_CLUSTER = key("damp_moss_cluster");
    public static final ResourceKey<Feature> UNDERWATER_SILT_PATCH = key("underwater_silt_patch");
    public static final ResourceKey<Feature> STICK_PATCH = key("stick_patch");
    public static final ResourceKey<Feature> STONE_SHORE = key("stone_shore");
    public static final ResourceKey<Feature> FOOTHILLS_POND = key("foothills_pond");
    public static final ResourceKey<Feature> MYCENA_PATCH = key("mycena_patch");

    public static final ResourceKey<Feature> DEPTHS_TUFF_ORE = key("depths_tuff_ore");
    public static final ResourceKey<Feature> DEPTHS_ANDESITE_ORE = key("depths_andesite_ore");

    private MainConfiguredFeatures() {}

    public static void bootstrap(BootstrapContext<Feature> context) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        context.register(PLATINUM_ORE, new OreFeature(List.of(
                BlockReplacement.replace(stone, MainBlocks.PLATINUM_ORE.get().defaultBlockState()),
                BlockReplacement.replace(deepslate, MainBlocks.DEEPSLATE_PLATINUM_ORE.get().defaultBlockState())), 4));
        context.register(RUBY_ORE, new OreFeature(List.of(
                BlockReplacement.replace(stone, MainBlocks.RUBY_ORE.get().defaultBlockState()),
                BlockReplacement.replace(deepslate, MainBlocks.DEEPSLATE_RUBY_ORE.get().defaultBlockState())), 8));
        context.register(SLATE_SHEET, new OreFeature(List.of(
                BlockReplacement.replace(stone, MainStones.SLATE.get().defaultBlockState())), 48, 0.2F));
        context.register(DEPTHS_TUFF_ORE, new OreFeature(deepslate, Blocks.TUFF.defaultBlockState(), 24));
        context.register(DEPTHS_ANDESITE_ORE, new OreFeature(deepslate, Blocks.ANDESITE.defaultBlockState(), 48));

        // Xenon's trees and bushes, built in code from its structure files.
        context.register(MAPLE_TREE, new MapleTreeFeature());
        context.register(MAPLE_BUSH, new MapleBushFeature());
        context.register(FIR_TREE, new FirTreeFeature());
        context.register(FIR_BUSH, new FirBushFeature());
        context.register(BIRCH_TREE, new BirchTreeFeature());
        context.register(YELLOW_BIRCH_TREE, new YellowBirchTreeFeature());
        context.register(OAK_BUSH, new OakBushFeature());

        // Its vanilla-style trees.
        Holder<BlockStateProvider> soil = context.lookup(Registries.BLOCK_STATE_PROVIDER).getOrThrow(BlockStateProviders.SOIL_BENEATH_TREE);
        context.register(MAPLE_OAK_TREE, new TreeFeature.Builder(BlockStateProvider.of(Blocks.OAK_LOG), new StraightTrunkPlacer(4, 2, 0),
                BlockStateProvider.of(Blocks.OAK_LEAVES), new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1), soil).ignoreVines().build());
        context.register(COLD_MAPLE_SPRUCE, new TreeFeature.Builder(BlockStateProvider.of(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(5, 3, 0),
                BlockStateProvider.of(Blocks.SPRUCE_LEAVES),
                new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
                new TwoLayersFeatureSize(2, 0, 2), soil).ignoreVines().build());
        context.register(MAPLE_BIRCH_TREE, new TreeFeature.Builder(BlockStateProvider.of(Blocks.BIRCH_LOG), new StraightTrunkPlacer(7, 4, 0),
                BlockStateProvider.of(Blocks.BIRCH_LEAVES), new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1), soil).ignoreVines().build());

        // Its random patches' blocks.
        simple(context, CLOVER_PATCH, BlockStateProvider.of(MainPlants.CLOVER.get()));
        simple(context, SHORT_GRASS_PATCH, weighted(Blocks.SHORT_GRASS, 3, Blocks.TALL_GRASS, 1));
        simple(context, FLOWER_PATCH, new WeightedStateProvider(WeightedList.<BlockState>builder()
                .add(Blocks.ROSE_BUSH.defaultBlockState(), 3)
                .add(Blocks.POPPY.defaultBlockState(), 3)
                .add(Blocks.AZURE_BLUET.defaultBlockState(), 2)
                .add(Blocks.OXEYE_DAISY.defaultBlockState(), 2)
                .add(Blocks.SUNFLOWER.defaultBlockState(), 4)));
        simple(context, PUMPKIN_PATCH, BlockStateProvider.of(Blocks.PUMPKIN));
        simple(context, MUSHROOM_PATCH, BlockStateProvider.of(Blocks.BROWN_MUSHROOM));
        simple(context, LILY_PAD_PATCH, BlockStateProvider.of(Blocks.LILY_PAD));
        simple(context, SMALL_LILY_PATCH, BlockStateProvider.of(MainPlants.SMALL_LILY.get()));
        simple(context, DRY_GRASS_PATCH, weighted(MainPlants.DRY_GRASS.get(), 3, MainPlants.TALL_DRY_GRASS.get(), 1));
        simple(context, FERN_PATCH, weighted(Blocks.FERN, 4, Blocks.LARGE_FERN, 1));
        simple(context, DANDELION_PATCH, BlockStateProvider.of(Blocks.DANDELION));
        simple(context, ORANGE_TULIP_PATCH, BlockStateProvider.of(Blocks.ORANGE_TULIP));
        simple(context, HIGHLAND_FLOWER_PATCH, new WeightedStateProvider(WeightedList.<BlockState>builder()
                .add(Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), 3)
                .add(Blocks.OXEYE_DAISY.defaultBlockState(), 3)
                .add(MainPlants.WHITE_AUBRIETA.get().defaultBlockState(), 2)));
        simple(context, ALLIUM_PATCH, weighted(Blocks.ALLIUM, 3, Blocks.PINK_TULIP, 2));
        simple(context, SUNFLOWER_PATCH, BlockStateProvider.of(Blocks.SUNFLOWER));
        simple(context, DENSE_PUMPKIN, BlockStateProvider.of(Blocks.PUMPKIN));
        simple(context, COLD_BERRY_BUSH, BlockStateProvider.of(Blocks.SWEET_BERRY_BUSH));
        simple(context, DEPTHS_MUSHROOM_PATCH, BlockStateProvider.of(Blocks.BROWN_MUSHROOM));

        context.register(DIORITE_MOUND, new StoneMoundFeature(Blocks.DIORITE.defaultBlockState(), false, true));
        context.register(ANDESITE_MOUND, new StoneMoundFeature(Blocks.ANDESITE.defaultBlockState(), false, true));
        context.register(GRANITE_MOUND, new StoneMoundFeature(Blocks.GRANITE.defaultBlockState(), false, true));
        context.register(SLATE_WATER_MOUND, new StoneMoundFeature(MainStones.SLATE.get().defaultBlockState(), true, false));
        context.register(SLATE_BOULDER, new SlateBoulderFeature());
        context.register(FALLEN_OAK_LOG, new FallenLogFeature());
        context.register(FALLEN_BIRCH_LOG, new FallenBirchLogFeature());
        context.register(CATTAILS, new WaterEdgeCattailFeature());
        context.register(WATER_SILT_DISK, new WaterEdgeDiskFeature());
        context.register(SURFACE_PATCH, new SurfacePatchFeature());
        context.register(LEAF_LITTER_PATCH, new LeafLitterFeature());
        context.register(COLD_LEAF_LITTER, new ColdLeafLitterFeature());
        context.register(SHELF_FUNGUS_CLUSTER, new ShelfFungusClusterFeature());
        context.register(PODZOL_PATCH, new PodzolPatchFeature());
        context.register(DAMP_MOSS_CLUSTER, new DampMossPatchFeature());
        context.register(UNDERWATER_SILT_PATCH, new UnderwaterSiltPatchFeature());
        context.register(STICK_PATCH, new StickPatchFeature());
        context.register(STONE_SHORE, new StoneShoreFeature());
        context.register(FOOTHILLS_POND, new FoothillsPondFeature());
        context.register(MYCENA_PATCH, new MycenaPatchFeature());
    }

    private static ResourceKey<Feature> key(String name) {
        return ResourceKey.create(Registries.FEATURE, ApeiraIds.id(name));
    }

    private static void simple(BootstrapContext<Feature> context, ResourceKey<Feature> key, BlockStateProvider provider) {
        context.register(key, new SimpleBlockFeature(provider));
    }

    private static WeightedStateProvider weighted(Block first, int firstWeight, Block second, int secondWeight) {
        return new WeightedStateProvider(WeightedList.<BlockState>builder()
                .add(first.defaultBlockState(), firstWeight)
                .add(second.defaultBlockState(), secondWeight));
    }
}
