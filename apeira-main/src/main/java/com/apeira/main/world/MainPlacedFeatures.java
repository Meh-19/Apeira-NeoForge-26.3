package com.apeira.main.world;

import java.util.ArrayList;
import java.util.List;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

/**
 * Xenon's placed features, with its ids and placements ({@code ModPlacedFeatures}), written by datagen as
 * {@code data/apeira/worldgen/placed_feature/}. A placed random patch ends with {@link #patch}'s modifiers. Xenon's
 * declared but never registered {@code depths_magma_patch}, {@code depths_cobweb_patch}, {@code depths_fossil} and
 * {@code orange_mycena_feature} are left out.
 */
public final class MainPlacedFeatures {
    public static final ResourceKey<PlacedFeature> PLATINUM_ORE = key("platinum_ore_placed");
    public static final ResourceKey<PlacedFeature> RUBY_ORE = key("ruby_ore_placed");
    public static final ResourceKey<PlacedFeature> SLATE_SHEET = key("slate_sheet");

    public static final ResourceKey<PlacedFeature> MAPLE_TREES = key("maple_trees");
    public static final ResourceKey<PlacedFeature> MAPLE_BUSHES = key("maple_bushes");
    public static final ResourceKey<PlacedFeature> RARE_BIRCH = key("rare_birch");
    public static final ResourceKey<PlacedFeature> COLD_MAPLE_SPRUCE = key("cold_maple_spruce");
    public static final ResourceKey<PlacedFeature> COLD_BIRCH_TREES = key("cold_birch_trees");
    public static final ResourceKey<PlacedFeature> YELLOW_BIRCH_ACCENT = key("yellow_birch_accent");
    public static final ResourceKey<PlacedFeature> BIRCH_FOREST_TREES = key("birch_forest_trees");
    public static final ResourceKey<PlacedFeature> YELLOW_BIRCH_PATCH = key("yellow_birch_patch");
    public static final ResourceKey<PlacedFeature> YELLOW_BIRCH_TREES = key("yellow_birch_trees");
    public static final ResourceKey<PlacedFeature> BIRCH_TREE = key("birch_tree");
    public static final ResourceKey<PlacedFeature> MAPLE_OAK = key("maple_oak");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_FIR_TREES = key("foothills_fir_trees");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_OAK = key("foothills_oak");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_BIRCH = key("foothills_birch");
    public static final ResourceKey<PlacedFeature> FIR_BUSHES = key("fir_bushes");
    public static final ResourceKey<PlacedFeature> OAK_BUSHES = key("oak_bushes");

    public static final ResourceKey<PlacedFeature> FALLEN_BIRCH_LOG = key("fallen_birch_log");
    public static final ResourceKey<PlacedFeature> STICK_PATCH = key("stick_patch");
    public static final ResourceKey<PlacedFeature> ALLIUM_PATCH = key("allium_patch");
    public static final ResourceKey<PlacedFeature> SUNFLOWER_PATCH = key("sunflower_patch");
    public static final ResourceKey<PlacedFeature> DENSE_PUMPKIN = key("dense_pumpkin");
    public static final ResourceKey<PlacedFeature> COLD_LEAF_LITTER = key("cold_leaf_litter");
    public static final ResourceKey<PlacedFeature> COLD_BERRY_BUSH = key("cold_berry_bush");
    public static final ResourceKey<PlacedFeature> CLOVER_PATCH = key("clover_patch");
    public static final ResourceKey<PlacedFeature> SHORT_GRASS_PATCH = key("short_grass_patch");
    public static final ResourceKey<PlacedFeature> FLOWER_PATCH = key("flower_patch");
    public static final ResourceKey<PlacedFeature> MYCENA_PATCH = key("mycena_patch");
    public static final ResourceKey<PlacedFeature> MUSHROOM_PATCH = key("mushroom_patch");
    public static final ResourceKey<PlacedFeature> PUMPKIN_PATCH = key("pumpkin_patch");
    public static final ResourceKey<PlacedFeature> LILY_PAD_PATCH = key("lily_pad_patch");
    public static final ResourceKey<PlacedFeature> SMALL_LILY_PATCH = key("small_lily_patch");
    public static final ResourceKey<PlacedFeature> DRY_GRASS_PATCH = key("dry_grass_patch");
    public static final ResourceKey<PlacedFeature> SURFACE_PATCH = key("surface_patch");
    public static final ResourceKey<PlacedFeature> SHELF_FUNGUS_CLUSTER = key("shelf_fungus_cluster");
    public static final ResourceKey<PlacedFeature> LEAF_LITTER_PATCH = key("leaf_litter_patch");
    public static final ResourceKey<PlacedFeature> FALLEN_OAK_LOG = key("fallen_oak_log");
    public static final ResourceKey<PlacedFeature> CATTAILS = key("cattails");
    public static final ResourceKey<PlacedFeature> WATER_SILT_DISK = key("water_silt_disk");
    public static final ResourceKey<PlacedFeature> DIORITE_MOUND = key("diorite_mound");
    public static final ResourceKey<PlacedFeature> ANDESITE_MOUND = key("andesite_mound");
    public static final ResourceKey<PlacedFeature> GRANITE_MOUND = key("granite_mound");
    public static final ResourceKey<PlacedFeature> SLATE_WATER_MOUND = key("slate_water_mound");
    public static final ResourceKey<PlacedFeature> FERN_PATCH = key("fern_patch");
    public static final ResourceKey<PlacedFeature> DANDELION_PATCH = key("dandelion_patch");
    public static final ResourceKey<PlacedFeature> ORANGE_TULIP_PATCH = key("orange_tulip_patch");
    public static final ResourceKey<PlacedFeature> HIGHLAND_FLOWER_PATCH = key("highland_flower_patch");
    public static final ResourceKey<PlacedFeature> PODZOL_PATCH = key("podzol_patch");
    public static final ResourceKey<PlacedFeature> DAMP_MOSS_CLUSTER = key("damp_moss_cluster");
    public static final ResourceKey<PlacedFeature> UNDERWATER_SILT_PATCH = key("underwater_silt_patch");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_MYCENA = key("foothills_mycena");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_ANDESITE_MOUND = key("foothills_andesite_mound");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_DIORITE_MOUND = key("foothills_diorite_mound");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_GRANITE_MOUND = key("foothills_granite_mound");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_SLATE_MOUND = key("foothills_slate_mound");
    public static final ResourceKey<PlacedFeature> SLATE_BOULDER = key("slate_boulder");
    public static final ResourceKey<PlacedFeature> RIVER_SLATE_MOUND = key("river_slate_mound");
    public static final ResourceKey<PlacedFeature> RIVER_SLATE_BOULDER = key("river_slate_boulder");
    public static final ResourceKey<PlacedFeature> STONE_SHORE = key("stone_shore");
    public static final ResourceKey<PlacedFeature> FOOTHILLS_POND = key("foothills_pond");

    public static final ResourceKey<PlacedFeature> DEPTHS_MUSHROOM_PATCH = key("depths_mushroom_patch");
    public static final ResourceKey<PlacedFeature> DEPTHS_ANDESITE_ORE = key("depths_andesite_ore");
    public static final ResourceKey<PlacedFeature> DEPTHS_TUFF_ORE = key("depths_tuff_ore");

    private static final PlacementModifier WORLD_SURFACE = PlacementUtils.HEIGHTMAP_WORLD_SURFACE;
    private static final PlacementModifier OCEAN_FLOOR = PlacementUtils.HEIGHTMAP_TOP_SOLID;
    private static final PlacementModifier DRY = SurfaceWaterDepthFilter.forMaxDepth(0);

    private MainPlacedFeatures() {}

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
        Placer place = (key, feature, modifiers) -> PlacementUtils.register(context, key, features.getOrThrow(feature), modifiers);

        place.at(RUBY_ORE, MainConfiguredFeatures.RUBY_ORE, List.of(CountPlacement.of(8), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-80), VerticalAnchor.absolute(80)), BiomeFilter.biome()));
        place.at(PLATINUM_ORE, MainConfiguredFeatures.PLATINUM_ORE, List.of(CountPlacement.of(4), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-80), VerticalAnchor.absolute(30)), BiomeFilter.biome()));
        place.at(SLATE_SHEET, MainConfiguredFeatures.SLATE_SHEET, List.of(CountPlacement.of(4),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(32)), InSquarePlacement.spread(),
                BiomeFilter.biome()));

        // Trees and bushes stay out of water.
        place.at(MAPLE_TREES, MainConfiguredFeatures.MAPLE_TREE, tree(PlacementUtils.countExtra(8, 0.1F, 1)));
        place.at(MAPLE_BUSHES, MainConfiguredFeatures.MAPLE_BUSH, tree(PlacementUtils.countExtra(1, 0.1F, 1)));
        place.at(RARE_BIRCH, MainConfiguredFeatures.MAPLE_BIRCH_TREE, tree(RarityFilter.onAverageOnceEvery(32)));
        place.at(COLD_MAPLE_SPRUCE, MainConfiguredFeatures.COLD_MAPLE_SPRUCE, tree(RarityFilter.onAverageOnceEvery(2)));
        place.at(COLD_BIRCH_TREES, MainConfiguredFeatures.YELLOW_BIRCH_TREE, tree(PlacementUtils.countExtra(10, 0.25F, 1)));
        place.at(YELLOW_BIRCH_ACCENT, MainConfiguredFeatures.YELLOW_BIRCH_TREE, tree(RarityFilter.onAverageOnceEvery(3)));
        place.at(BIRCH_FOREST_TREES, MainConfiguredFeatures.BIRCH_TREE, tree(PlacementUtils.countExtra(8, 0.1F, 1)));
        place.at(YELLOW_BIRCH_PATCH, MainConfiguredFeatures.YELLOW_BIRCH_TREE, tree(RarityFilter.onAverageOnceEvery(5)));
        place.at(YELLOW_BIRCH_TREES, MainConfiguredFeatures.YELLOW_BIRCH_TREE, tree(PlacementUtils.countExtra(7, 0.1F, 1)));
        place.at(BIRCH_TREE, MainConfiguredFeatures.BIRCH_TREE, tree(PlacementUtils.countExtra(5, 0.1F, 1)));
        place.at(MAPLE_OAK, MainConfiguredFeatures.MAPLE_OAK_TREE, tree(RarityFilter.onAverageOnceEvery(8)));
        place.at(FOOTHILLS_FIR_TREES, MainConfiguredFeatures.FIR_TREE, tree(PlacementUtils.countExtra(14, 0.25F, 1)));
        place.at(FOOTHILLS_OAK, MainConfiguredFeatures.MAPLE_OAK_TREE, tree(RarityFilter.onAverageOnceEvery(40)));
        place.at(FOOTHILLS_BIRCH, MainConfiguredFeatures.MAPLE_BIRCH_TREE, tree(RarityFilter.onAverageOnceEvery(64)));
        place.at(FIR_BUSHES, MainConfiguredFeatures.FIR_BUSH, tree(RarityFilter.onAverageOnceEvery(4)));
        place.at(OAK_BUSHES, MainConfiguredFeatures.OAK_BUSH, tree(RarityFilter.onAverageOnceEvery(5)));
        place.at(FOOTHILLS_POND, MainConfiguredFeatures.FOOTHILLS_POND, tree(RarityFilter.onAverageOnceEvery(6)));

        place.at(FALLEN_BIRCH_LOG, MainConfiguredFeatures.FALLEN_BIRCH_LOG, surface(RarityFilter.onAverageOnceEvery(5), WORLD_SURFACE));
        place.at(STICK_PATCH, MainConfiguredFeatures.STICK_PATCH, surface(CountPlacement.of(2), WORLD_SURFACE));
        place.at(COLD_LEAF_LITTER, MainConfiguredFeatures.COLD_LEAF_LITTER, surface(CountPlacement.of(3), WORLD_SURFACE));
        place.at(SURFACE_PATCH, MainConfiguredFeatures.SURFACE_PATCH, surface(RarityFilter.onAverageOnceEvery(3), OCEAN_FLOOR));
        place.at(SHELF_FUNGUS_CLUSTER, MainConfiguredFeatures.SHELF_FUNGUS_CLUSTER, surface(RarityFilter.onAverageOnceEvery(5), WORLD_SURFACE));
        place.at(LEAF_LITTER_PATCH, MainConfiguredFeatures.LEAF_LITTER_PATCH, surface(CountPlacement.of(3), WORLD_SURFACE));
        place.at(FALLEN_OAK_LOG, MainConfiguredFeatures.FALLEN_OAK_LOG, surface(RarityFilter.onAverageOnceEvery(6), WORLD_SURFACE));
        place.at(CATTAILS, MainConfiguredFeatures.CATTAILS, surface(RarityFilter.onAverageOnceEvery(2), WORLD_SURFACE));
        place.at(WATER_SILT_DISK, MainConfiguredFeatures.WATER_SILT_DISK, surface(RarityFilter.onAverageOnceEvery(3), OCEAN_FLOOR));
        place.at(DIORITE_MOUND, MainConfiguredFeatures.DIORITE_MOUND, surface(RarityFilter.onAverageOnceEvery(8), WORLD_SURFACE));
        place.at(ANDESITE_MOUND, MainConfiguredFeatures.ANDESITE_MOUND, surface(RarityFilter.onAverageOnceEvery(8), WORLD_SURFACE));
        place.at(GRANITE_MOUND, MainConfiguredFeatures.GRANITE_MOUND, surface(RarityFilter.onAverageOnceEvery(8), WORLD_SURFACE));
        place.at(PODZOL_PATCH, MainConfiguredFeatures.PODZOL_PATCH, surface(RarityFilter.onAverageOnceEvery(24), WORLD_SURFACE));
        place.at(DAMP_MOSS_CLUSTER, MainConfiguredFeatures.DAMP_MOSS_CLUSTER, surface(CountPlacement.of(3), WORLD_SURFACE));
        place.at(UNDERWATER_SILT_PATCH, MainConfiguredFeatures.UNDERWATER_SILT_PATCH, surface(RarityFilter.onAverageOnceEvery(3), OCEAN_FLOOR));
        place.at(MYCENA_PATCH, MainConfiguredFeatures.MYCENA_PATCH, surface(RarityFilter.onAverageOnceEvery(6), WORLD_SURFACE));
        place.at(FOOTHILLS_MYCENA, MainConfiguredFeatures.MYCENA_PATCH, surface(RarityFilter.onAverageOnceEvery(12), WORLD_SURFACE));
        place.at(FOOTHILLS_ANDESITE_MOUND, MainConfiguredFeatures.ANDESITE_MOUND, surface(RarityFilter.onAverageOnceEvery(4), WORLD_SURFACE));
        place.at(FOOTHILLS_DIORITE_MOUND, MainConfiguredFeatures.DIORITE_MOUND, surface(RarityFilter.onAverageOnceEvery(12), WORLD_SURFACE));
        place.at(FOOTHILLS_GRANITE_MOUND, MainConfiguredFeatures.GRANITE_MOUND, surface(RarityFilter.onAverageOnceEvery(24), WORLD_SURFACE));
        place.at(FOOTHILLS_SLATE_MOUND, MainConfiguredFeatures.SLATE_BOULDER, surface(RarityFilter.onAverageOnceEvery(4), OCEAN_FLOOR));
        place.at(SLATE_BOULDER, MainConfiguredFeatures.SLATE_BOULDER, surface(RarityFilter.onAverageOnceEvery(5), OCEAN_FLOOR));
        place.at(RIVER_SLATE_BOULDER, MainConfiguredFeatures.SLATE_BOULDER, surface(RarityFilter.onAverageOnceEvery(3), OCEAN_FLOOR));
        place.at(STONE_SHORE, MainConfiguredFeatures.STONE_SHORE, surface(RarityFilter.onAverageOnceEvery(8), WORLD_SURFACE));
        place.at(SLATE_WATER_MOUND, MainConfiguredFeatures.SLATE_WATER_MOUND, List.of(RarityFilter.onAverageOnceEvery(10),
                InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(2), OCEAN_FLOOR, BiomeFilter.biome()));
        place.at(RIVER_SLATE_MOUND, MainConfiguredFeatures.SLATE_BOULDER, List.of(CountPlacement.of(2), InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(2), OCEAN_FLOOR, BiomeFilter.biome()));

        // Random patches: tries, horizontal spread and vertical spread.
        place.at(ALLIUM_PATCH, MainConfiguredFeatures.ALLIUM_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(4), WORLD_SURFACE), 28, 5, 0));
        place.at(SUNFLOWER_PATCH, MainConfiguredFeatures.SUNFLOWER_PATCH,
                patch(surface(RarityFilter.onAverageOnceEvery(5), WORLD_SURFACE), 24, 5, 0));
        place.at(DENSE_PUMPKIN, MainConfiguredFeatures.DENSE_PUMPKIN, patch(surface(RarityFilter.onAverageOnceEvery(2), WORLD_SURFACE), 40, 7, 0));
        place.at(COLD_BERRY_BUSH, MainConfiguredFeatures.COLD_BERRY_BUSH,
                patch(surface(RarityFilter.onAverageOnceEvery(6), WORLD_SURFACE), 12, 4, 0));
        place.at(CLOVER_PATCH, MainConfiguredFeatures.CLOVER_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(4), WORLD_SURFACE), 64, 9, 0));
        place.at(SHORT_GRASS_PATCH, MainConfiguredFeatures.SHORT_GRASS_PATCH, patch(surface(CountPlacement.of(6), WORLD_SURFACE), 64, 5, 0));
        place.at(FLOWER_PATCH, MainConfiguredFeatures.FLOWER_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(3), WORLD_SURFACE), 32, 6, 2));
        place.at(MUSHROOM_PATCH, MainConfiguredFeatures.MUSHROOM_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(5), WORLD_SURFACE), 16, 4, 2));
        place.at(PUMPKIN_PATCH, MainConfiguredFeatures.PUMPKIN_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(4), OCEAN_FLOOR), 20, 6, 0));
        place.at(LILY_PAD_PATCH, MainConfiguredFeatures.LILY_PAD_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(3), WORLD_SURFACE), 10, 7, 2));
        place.at(SMALL_LILY_PATCH, MainConfiguredFeatures.SMALL_LILY_PATCH,
                patch(surface(RarityFilter.onAverageOnceEvery(4), WORLD_SURFACE), 8, 5, 2));
        place.at(DRY_GRASS_PATCH, MainConfiguredFeatures.DRY_GRASS_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(3), WORLD_SURFACE), 32, 5, 0));
        place.at(FERN_PATCH, MainConfiguredFeatures.FERN_PATCH, patch(surface(CountPlacement.of(4), WORLD_SURFACE), 64, 6, 0));
        place.at(DANDELION_PATCH, MainConfiguredFeatures.DANDELION_PATCH, patch(surface(RarityFilter.onAverageOnceEvery(2), WORLD_SURFACE), 32, 5, 0));
        place.at(ORANGE_TULIP_PATCH, MainConfiguredFeatures.ORANGE_TULIP_PATCH,
                patch(surface(RarityFilter.onAverageOnceEvery(4), WORLD_SURFACE), 24, 5, 0));
        place.at(HIGHLAND_FLOWER_PATCH, MainConfiguredFeatures.HIGHLAND_FLOWER_PATCH,
                patch(surface(RarityFilter.onAverageOnceEvery(3), WORLD_SURFACE), 32, 6, 1));

        // The Depths, from the bottom of the world to y 0.
        HeightRangePlacement depths = HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(0));
        place.at(DEPTHS_MUSHROOM_PATCH, MainConfiguredFeatures.DEPTHS_MUSHROOM_PATCH,
                patch(List.of(RarityFilter.onAverageOnceEvery(8), InSquarePlacement.spread(), depths, BiomeFilter.biome()), 12, 3, 2));
        place.at(DEPTHS_ANDESITE_ORE, MainConfiguredFeatures.DEPTHS_ANDESITE_ORE,
                List.of(CountPlacement.of(8), InSquarePlacement.spread(), depths, BiomeFilter.biome()));
        place.at(DEPTHS_TUFF_ORE, MainConfiguredFeatures.DEPTHS_TUFF_ORE,
                List.of(CountPlacement.of(6), InSquarePlacement.spread(), depths, BiomeFilter.biome()));
    }

    /**
     * {@code modifiers} followed by what Xenon's {@code random_patch} did: {@code tries} positions spread around each
     * position by {@code xzSpread} and {@code ySpread} blocks (a triangular distribution), each kept only in air.
     */
    public static List<PlacementModifier> patch(List<PlacementModifier> modifiers, int tries, int xzSpread, int ySpread) {
        List<PlacementModifier> patch = new ArrayList<>(modifiers);
        patch.add(CountPlacement.of(tries));
        patch.add(OffsetPlacement.ofTriangle(xzSpread, ySpread));
        patch.add(BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE));
        return patch;
    }

    private static List<PlacementModifier> tree(PlacementModifier count) {
        return List.of(count, InSquarePlacement.spread(), DRY, WORLD_SURFACE, BiomeFilter.biome());
    }

    private static List<PlacementModifier> surface(PlacementModifier count, PlacementModifier heightmap) {
        return List.of(count, InSquarePlacement.spread(), heightmap, BiomeFilter.biome());
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ApeiraIds.id(name));
    }

    @FunctionalInterface
    private interface Placer {
        void at(ResourceKey<PlacedFeature> key, ResourceKey<Feature> feature, List<PlacementModifier> modifiers);
    }
}
