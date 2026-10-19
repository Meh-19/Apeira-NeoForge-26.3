package com.apeira.main.world;

import java.util.Set;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * What Xenon's Fabric biome modifications did to vanilla biomes, as NeoForge biome modifiers
 * ({@code data/apeira/neoforge/biome_modifier/}): platinum, ruby and slate veins in every overworld biome, slate boulders
 * in rivers, and its own birch trees in place of vanilla's in birch forests. Xenon's own biomes have these in their
 * definitions ({@link MainBiomes}); they are not in {@code #minecraft:is_overworld}, so nothing is added twice.
 */
public final class MainBiomeModifiers {
    public static final ResourceKey<BiomeModifier> OVERWORLD_ORES = key("overworld_ores");
    public static final ResourceKey<BiomeModifier> RIVER_SLATE_BOULDERS = key("river_slate_boulders");
    public static final ResourceKey<BiomeModifier> REMOVE_VANILLA_BIRCHES = key("remove_vanilla_birches");
    public static final ResourceKey<BiomeModifier> BIRCH_FOREST_TREES = key("birch_forest_trees");

    private MainBiomeModifiers() {}

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> birchForests = HolderSet.direct(biomes.getOrThrow(Biomes.BIRCH_FOREST), biomes.getOrThrow(Biomes.OLD_GROWTH_BIRCH_FOREST));

        context.register(OVERWORLD_ORES, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(features.getOrThrow(MainPlacedFeatures.PLATINUM_ORE), features.getOrThrow(MainPlacedFeatures.RUBY_ORE),
                        features.getOrThrow(MainPlacedFeatures.SLATE_SHEET)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        context.register(RIVER_SLATE_BOULDERS, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.RIVER), biomes.getOrThrow(Biomes.FROZEN_RIVER)),
                HolderSet.direct(features.getOrThrow(MainPlacedFeatures.RIVER_SLATE_BOULDER)), GenerationStep.Decoration.VEGETAL_DECORATION));
        context.register(REMOVE_VANILLA_BIRCHES, new BiomeModifiers.RemoveFeaturesBiomeModifier(birchForests,
                HolderSet.direct(features.getOrThrow(VegetationPlacements.TREES_BIRCH), features.getOrThrow(VegetationPlacements.BIRCH_TALL)),
                Set.of(GenerationStep.Decoration.VEGETAL_DECORATION)));
        context.register(BIRCH_FOREST_TREES, new BiomeModifiers.AddFeaturesBiomeModifier(birchForests,
                HolderSet.direct(features.getOrThrow(MainPlacedFeatures.BIRCH_FOREST_TREES), features.getOrThrow(MainPlacedFeatures.YELLOW_BIRCH_PATCH)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static ResourceKey<BiomeModifier> key(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ApeiraIds.id(name));
    }
}
