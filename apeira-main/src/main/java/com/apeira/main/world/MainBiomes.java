package com.apeira.main.world;

import java.util.List;
import java.util.Optional;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainSounds;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.ListModifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Xenon's five biomes, with its ids, climate, colours and spawns ({@code ModBiomes}), written by datagen as
 * {@code data/apeira/worldgen/biome/}. Each biome's features are its own (Xenon's surface biomes had none but the cold
 * birch forest's carvers and lakes) followed by those Xenon added with Fabric's biome modifications, which ran in the
 * order of their ids: vanilla's first, then Xenon's by name. {@link MainBiomePlacement} puts them into the overworld.
 */
public final class MainBiomes {
    public static final ResourceKey<Biome> MAPLE_FOREST = key("maple_forest");
    public static final ResourceKey<Biome> FOOTHILLS = key("foothills");
    public static final ResourceKey<Biome> COLD_MAPLE_FOREST = key("cold_maple_forest");
    public static final ResourceKey<Biome> COLD_BIRCH_FOREST = key("cold_birch_forest");
    public static final ResourceKey<Biome> THE_DEPTHS = key("the_depths");
    /** Every biome above, in that order. */
    public static final List<ResourceKey<Biome>> ALL = List.of(MAPLE_FOREST, FOOTHILLS, COLD_MAPLE_FOREST, COLD_BIRCH_FOREST, THE_DEPTHS);

    private static final GenerationStep.Decoration ORES = GenerationStep.Decoration.UNDERGROUND_ORES;
    private static final GenerationStep.Decoration VEGETAL = GenerationStep.Decoration.VEGETAL_DECORATION;
    private static final GenerationStep.Decoration TOP_LAYER = GenerationStep.Decoration.TOP_LAYER_MODIFICATION;
    private static final GenerationStep.Decoration LOCAL = GenerationStep.Decoration.LOCAL_MODIFICATIONS;

    private MainBiomes() {}

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<WorldCarver> carvers = context.lookup(Registries.CARVER);
        context.register(MAPLE_FOREST, mapleForest(features, carvers));
        context.register(FOOTHILLS, foothills(features, carvers));
        context.register(COLD_MAPLE_FOREST, coldMapleForest(features, carvers));
        context.register(COLD_BIRCH_FOREST, coldBirchForest(features, carvers));
        context.register(THE_DEPTHS, theDepths(features, carvers));
    }

    private static Biome mapleForest(HolderGetter<PlacedFeature> features, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder spawns = surfaceSpawns();
        spawns.addSpawn(EntityTypes.WOLF, 5, 2, 4);
        spawns.addSpawn(EntityTypes.FOX, 4, 2, 4);
        spawns.addSpawn(MainEntities.TURKEY.get(), 25, 1, 3);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addOres(generation);
        generation.addFeature(VEGETAL, VegetationPlacements.PATCH_PUMPKIN);
        add(generation, VEGETAL, MainPlacedFeatures.CATTAILS, MainPlacedFeatures.CLOVER_PATCH, MainPlacedFeatures.DRY_GRASS_PATCH,
                MainPlacedFeatures.FALLEN_OAK_LOG, MainPlacedFeatures.FLOWER_PATCH, MainPlacedFeatures.FOOTHILLS_SLATE_MOUND,
                MainPlacedFeatures.LEAF_LITTER_PATCH, MainPlacedFeatures.LILY_PAD_PATCH, MainPlacedFeatures.MAPLE_BUSHES,
                MainPlacedFeatures.MAPLE_OAK, MainPlacedFeatures.MAPLE_TREES, MainPlacedFeatures.MUSHROOM_PATCH,
                MainPlacedFeatures.MYCENA_PATCH, MainPlacedFeatures.RARE_BIRCH, MainPlacedFeatures.SHELF_FUNGUS_CLUSTER,
                MainPlacedFeatures.SHORT_GRASS_PATCH, MainPlacedFeatures.SMALL_LILY_PATCH);
        add(generation, TOP_LAYER, MainPlacedFeatures.SURFACE_PATCH, MainPlacedFeatures.WATER_SILT_DISK);
        add(generation, LOCAL, MainPlacedFeatures.ANDESITE_MOUND, MainPlacedFeatures.DIORITE_MOUND, MainPlacedFeatures.GRANITE_MOUND,
                MainPlacedFeatures.SLATE_WATER_MOUND);
        return biome(0.7F, 7972607, 12638463, 4161252, 329011, 10335557, 11914577, spawns, generation).build();
    }

    private static Biome foothills(HolderGetter<PlacedFeature> features, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder spawns = surfaceSpawns();
        spawns.addSpawn(EntityTypes.WOLF, 5, 2, 4);
        spawns.addSpawn(EntityTypes.FOX, 4, 2, 3);
        spawns.addSpawn(EntityTypes.RABBIT, 4, 2, 3);
        spawns.addSpawn(MainEntities.GRIZZLY_BEAR.get(), 20, 1, 3);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addOres(generation);
        generation.addFeature(VEGETAL, MiscOverworldPlacements.DISK_CLAY);
        generation.addFeature(VEGETAL, MiscOverworldPlacements.DISK_GRAVEL);
        generation.addFeature(VEGETAL, MiscOverworldPlacements.DISK_SAND);
        add(generation, VEGETAL, MainPlacedFeatures.CATTAILS, MainPlacedFeatures.CLOVER_PATCH, MainPlacedFeatures.DAMP_MOSS_CLUSTER,
                MainPlacedFeatures.DANDELION_PATCH, MainPlacedFeatures.FALLEN_OAK_LOG, MainPlacedFeatures.FERN_PATCH,
                MainPlacedFeatures.FIR_BUSHES, MainPlacedFeatures.FOOTHILLS_BIRCH, MainPlacedFeatures.FOOTHILLS_FIR_TREES,
                MainPlacedFeatures.FOOTHILLS_MYCENA, MainPlacedFeatures.FOOTHILLS_OAK, MainPlacedFeatures.FOOTHILLS_SLATE_MOUND,
                MainPlacedFeatures.HIGHLAND_FLOWER_PATCH, MainPlacedFeatures.LEAF_LITTER_PATCH, MainPlacedFeatures.LILY_PAD_PATCH,
                MainPlacedFeatures.MUSHROOM_PATCH, MainPlacedFeatures.ORANGE_TULIP_PATCH, MainPlacedFeatures.SHELF_FUNGUS_CLUSTER,
                MainPlacedFeatures.SHORT_GRASS_PATCH, MainPlacedFeatures.SMALL_LILY_PATCH, MainPlacedFeatures.UNDERWATER_SILT_PATCH);
        add(generation, TOP_LAYER, MainPlacedFeatures.PODZOL_PATCH, MainPlacedFeatures.SURFACE_PATCH, MainPlacedFeatures.WATER_SILT_DISK);
        add(generation, LOCAL, MainPlacedFeatures.FOOTHILLS_ANDESITE_MOUND, MainPlacedFeatures.FOOTHILLS_DIORITE_MOUND,
                MainPlacedFeatures.FOOTHILLS_GRANITE_MOUND, MainPlacedFeatures.FOOTHILLS_POND, MainPlacedFeatures.STONE_SHORE);
        return biome(0.25F, 9285856, 14933710, 4166116, 329011, 10533476, 8164941, spawns, generation).build();
    }

    private static Biome coldMapleForest(HolderGetter<PlacedFeature> features, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder spawns = surfaceSpawns();
        spawns.addSpawn(EntityTypes.WOLF, 5, 2, 4);
        spawns.addSpawn(EntityTypes.FOX, 6, 2, 4);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addOres(generation);
        add(generation, VEGETAL, MainPlacedFeatures.CATTAILS, MainPlacedFeatures.CLOVER_PATCH, MainPlacedFeatures.COLD_BERRY_BUSH,
                MainPlacedFeatures.COLD_LEAF_LITTER, MainPlacedFeatures.COLD_MAPLE_SPRUCE, MainPlacedFeatures.DRY_GRASS_PATCH,
                MainPlacedFeatures.FALLEN_OAK_LOG, MainPlacedFeatures.FLOWER_PATCH, MainPlacedFeatures.FOOTHILLS_SLATE_MOUND,
                MainPlacedFeatures.LILY_PAD_PATCH, MainPlacedFeatures.MAPLE_BUSHES, MainPlacedFeatures.MAPLE_TREES,
                MainPlacedFeatures.MUSHROOM_PATCH, MainPlacedFeatures.MYCENA_PATCH, MainPlacedFeatures.SHELF_FUNGUS_CLUSTER,
                MainPlacedFeatures.SHORT_GRASS_PATCH, MainPlacedFeatures.SMALL_LILY_PATCH);
        add(generation, TOP_LAYER, MainPlacedFeatures.SURFACE_PATCH, MainPlacedFeatures.WATER_SILT_DISK);
        add(generation, LOCAL, MainPlacedFeatures.ANDESITE_MOUND, MainPlacedFeatures.DIORITE_MOUND, MainPlacedFeatures.GRANITE_MOUND,
                MainPlacedFeatures.SLATE_WATER_MOUND);
        return biome(0.7F, 10466802, 15456934, 4161252, 329011, 11648868, 10596931, spawns, generation).build();
    }

    private static Biome coldBirchForest(HolderGetter<PlacedFeature> features, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder spawns = surfaceSpawns();
        spawns.addSpawn(EntityTypes.FOX, 5, 2, 4);
        spawns.addSpawn(EntityTypes.RABBIT, 4, 2, 4);
        spawns.addSpawn(EntityTypes.BEE, 3, 2, 3);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        addOres(generation);
        add(generation, VEGETAL, MainPlacedFeatures.ALLIUM_PATCH, MainPlacedFeatures.CATTAILS, MainPlacedFeatures.CLOVER_PATCH,
                MainPlacedFeatures.COLD_BIRCH_TREES, MainPlacedFeatures.DAMP_MOSS_CLUSTER, MainPlacedFeatures.DANDELION_PATCH,
                MainPlacedFeatures.DRY_GRASS_PATCH, MainPlacedFeatures.FALLEN_BIRCH_LOG, MainPlacedFeatures.FALLEN_OAK_LOG,
                MainPlacedFeatures.FOOTHILLS_SLATE_MOUND, MainPlacedFeatures.HIGHLAND_FLOWER_PATCH, MainPlacedFeatures.LEAF_LITTER_PATCH,
                MainPlacedFeatures.LILY_PAD_PATCH, MainPlacedFeatures.MUSHROOM_PATCH, MainPlacedFeatures.MYCENA_PATCH,
                MainPlacedFeatures.OAK_BUSHES, MainPlacedFeatures.SHELF_FUNGUS_CLUSTER, MainPlacedFeatures.SHORT_GRASS_PATCH,
                MainPlacedFeatures.SMALL_LILY_PATCH, MainPlacedFeatures.STICK_PATCH, MainPlacedFeatures.SUNFLOWER_PATCH,
                MainPlacedFeatures.UNDERWATER_SILT_PATCH);
        add(generation, TOP_LAYER, MainPlacedFeatures.SURFACE_PATCH, MainPlacedFeatures.WATER_SILT_DISK);
        add(generation, LOCAL, MainPlacedFeatures.ANDESITE_MOUND, MainPlacedFeatures.DIORITE_MOUND, MainPlacedFeatures.FOOTHILLS_POND,
                MainPlacedFeatures.GRANITE_MOUND, MainPlacedFeatures.PUMPKIN_PATCH);
        return biome(0.6F, 10466802, 15456934, 4159204, 329011, 10335557, 10732103, spawns, generation).build();
    }

    private static Biome theDepths(HolderGetter<PlacedFeature> features, HolderGetter<WorldCarver> carvers) {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(spawns);
        spawns.addSpawn(EntityTypes.CAVE_SPIDER, 10, 1, 3);
        spawns.addSpawn(MainEntities.WRAITH.get(), 15, 1, 1);
        // Xenon's biome modifications added the cave spiders a second time.
        spawns.addSpawn(EntityTypes.CAVE_SPIDER, 10, 1, 3);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        // Xenon also added the underground lava lakes and lava springs, which the biome already has: a feature can only be
        // in a step once.
        add(generation, ORES, MainPlacedFeatures.DEPTHS_ANDESITE_ORE, MainPlacedFeatures.DEPTHS_TUFF_ORE);
        addOres(generation);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, CavePlacements.FOSSIL_LOWER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, MainPlacedFeatures.DEPTHS_MUSHROOM_PATCH);

        return biome(0.8F, 8233983, 2886411, 3111796, 2592367, 8428613, 10466622, spawns, generation)
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, new AmbientSounds(Optional.of(MainSounds.DEPTHS_AMBIENT),
                        Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS), List.of()))
                .modifyAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, ListModifier.append(), AmbientParticle.of(ParticleTypes.WHITE_ASH, 0.01F))
                .build();
    }

    /** Xenon's surface biome spawns: farm animals, bats and the common monsters, plus each biome's own. */
    private static MobSpawnSettings.Builder surfaceSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(spawns);
        BiomeDefaultFeatures.commonSpawns(spawns);
        return spawns;
    }

    /** The platinum, ruby and slate veins Xenon added to every overworld biome, its own included. */
    private static void addOres(BiomeGenerationSettings.Builder generation) {
        add(generation, ORES, MainPlacedFeatures.PLATINUM_ORE, MainPlacedFeatures.RUBY_ORE, MainPlacedFeatures.SLATE_SHEET);
    }

    @SafeVarargs
    private static void add(BiomeGenerationSettings.Builder generation, GenerationStep.Decoration step, ResourceKey<PlacedFeature>... features) {
        for (ResourceKey<PlacedFeature> feature : features) {
            generation.addFeature(step, feature);
        }
    }

    /**
     * A biome with Xenon's precipitation, no downfall, creature spawn chance 0.1 and the cave mood sound, and the given
     * temperature and colours.
     */
    private static Biome.BiomeBuilder biome(float temperature, int sky, int fog, int water, int waterFog, int grass, int foliage,
            MobSpawnSettings.Builder spawns, BiomeGenerationSettings.Builder generation) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(temperature)
                .downfall(0.0F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(sky))
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(fog))
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(waterFog))
                .setAttribute(EnvironmentAttributes.CREATURE_WORLD_GEN_SPAWN_PROBABILITY, 0.1F)
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(water).grassColorOverride(grass).foliageColorOverride(foliage).build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(generation.build());
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ApeiraIds.id(name));
    }
}
