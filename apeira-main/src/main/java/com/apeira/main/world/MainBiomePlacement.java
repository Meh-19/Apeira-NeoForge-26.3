package com.apeira.main.world;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * How Xenon's biomes get into the overworld without TerraBlender or Biolith, which Xenon used and which do not exist
 * for 26.3. {@code mixin/OverworldBiomesMixin} lists them in the overworld preset at climate points no world reaches,
 * so they are among its possible biomes (feature order, {@code /locate}, structure checks), and
 * {@code mixin/MultiNoiseBiomeSourceMixin} passes every biome the overworld's source picks through {@link Placement#wrap}:
 * <ul>
 * <li>Below y -48 (quart y -12) everything is The Depths, as Xenon's {@code DepthsBiomeMixin} did, and so is the deep
 * dark wherever it is, as Xenon's Biolith replacement did.</li>
 * <li>Above it, the world is split into regions about 512 blocks across with wavy edges, each Xenon's TerraBlender
 * regions or vanilla's, with TerraBlender's weights: vanilla 10, each of Xenon's four 4. Inside one of Xenon's regions
 * the vanilla biomes it replaced become its biome: forest becomes maple forest; forest and the windswept hills,
 * gravelly hills and forest become foothills; taiga and old growth pine taiga become cold maple forest; old growth
 * birch forest becomes cold birch forest.</li>
 * </ul>
 * The terrain is still vanilla's for the replaced biome (TerraBlender kept vanilla's climate parameters too), and the
 * regions are this mod's own shapes rather than TerraBlender's.
 */
public final class MainBiomePlacement {
    /** Quart y below which everything is The Depths: block y -48. */
    private static final int DEPTHS_TOP_QUART = -12;
    /** Region cells, in quarts: 128 quarts are 512 blocks. */
    private static final int REGION_SIZE = 128;
    /** How far region edges wander, in quarts, and the size of their waves. */
    private static final int WARP = 24;
    private static final int WARP_SCALE = 32;
    /** TerraBlender's weights: vanilla's region, then Xenon's maple, foothills, cold maple and cold birch regions. */
    private static final int[] REGION_WEIGHTS = {10, 4, 4, 4, 4};
    private static final int TOTAL_WEIGHT = 26;

    private MainBiomePlacement() {}

    /** Adds Xenon's biomes to the overworld preset at climate points far outside the range any world samples. */
    public static void addUnreachablePoints(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        for (int i = 0; i < MainBiomes.ALL.size(); i++) {
            float far = 3.0F + i * 0.1F;
            biomes.accept(Pair.of(Climate.parameters(far, far, far, far, far, far, 1.0F), MainBiomes.ALL.get(i)));
        }
    }

    /**
     * Xenon's replacements for a biome source; empty (and passing biomes through) for a source that does not list
     * The Depths, such as the Nether's.
     */
    public static final class Placement {
        private static final Placement NONE = new Placement(null, null, Map.of());

        private final @Nullable Holder<Biome> depths;
        private final @Nullable Holder<Biome> deepDark;
        /** For each vanilla biome Xenon replaced, its replacement in each region (null: kept). */
        private final Map<Holder<Biome>, @Nullable Holder<Biome>[]> replacements;
        private volatile @Nullable Salt salt;

        private Placement(@Nullable Holder<Biome> depths, @Nullable Holder<Biome> deepDark, Map<Holder<Biome>, @Nullable Holder<Biome>[]> replacements) {
            this.depths = depths;
            this.deepDark = deepDark;
            this.replacements = replacements;
        }

        public static Placement of(Set<Holder<Biome>> possibleBiomes) {
            Map<ResourceKey<Biome>, Holder<Biome>> byKey = new HashMap<>();
            possibleBiomes.forEach(biome -> biome.unwrapKey().ifPresent(key -> byKey.put(key, biome)));
            Holder<Biome> depths = byKey.get(MainBiomes.THE_DEPTHS);
            if (depths == null) {
                return NONE;
            }
            Map<Holder<Biome>, @Nullable Holder<Biome>[]> replacements = new IdentityHashMap<>();
            replace(replacements, byKey, 1, MainBiomes.MAPLE_FOREST, List.of(Biomes.FOREST));
            replace(replacements, byKey, 2, MainBiomes.FOOTHILLS,
                    List.of(Biomes.FOREST, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST));
            replace(replacements, byKey, 3, MainBiomes.COLD_MAPLE_FOREST, List.of(Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA));
            replace(replacements, byKey, 4, MainBiomes.COLD_BIRCH_FOREST, List.of(Biomes.OLD_GROWTH_BIRCH_FOREST));
            return new Placement(depths, byKey.get(Biomes.DEEP_DARK), replacements);
        }

        @SuppressWarnings("unchecked")
        private static void replace(Map<Holder<Biome>, @Nullable Holder<Biome>[]> replacements, Map<ResourceKey<Biome>, Holder<Biome>> byKey,
                int region, ResourceKey<Biome> biome, List<ResourceKey<Biome>> vanilla) {
            Holder<Biome> replacement = byKey.get(biome);
            for (ResourceKey<Biome> key : vanilla) {
                Holder<Biome> original = byKey.get(key);
                if (original != null && replacement != null) {
                    replacements.computeIfAbsent(original, o -> new Holder[REGION_WEIGHTS.length])[region] = replacement;
                }
            }
        }

        /**
         * The seed of this world's regions: its own temperature and humidity noise far away from anything, so each
         * world seed has its own layout. Kept for as long as the source is used with the same noise.
         */
        private long salt(Climate.Sampler sampler) {
            Salt salt = this.salt;
            Object noise = sampler.temperature().sampler();
            if (salt == null || salt.noise() != noise) {
                long value = Float.floatToIntBits(sampler.temperature().sampleValue(1 << 22, 0, -(1 << 22))) * 0x9E3779B97F4A7C15L
                        ^ Float.floatToIntBits(sampler.humidity().sampleValue(-(1 << 22), 0, 1 << 22));
                salt = new Salt(noise, value);
                this.salt = salt;
            }
            return salt.value();
        }

        /** {@code resolver} with Xenon's biomes put in; {@code sampler} gives the world its own region layout. */
        public BiomeResolver wrap(BiomeResolver resolver, Climate.Sampler sampler) {
            Holder<Biome> depths = this.depths;
            if (depths == null) {
                return resolver;
            }
            long salt = salt(sampler);
            return (quartX, quartY, quartZ) -> {
                Holder<Biome> biome = resolver.getNoiseBiome(quartX, quartY, quartZ);
                if (quartY < DEPTHS_TOP_QUART || biome == deepDark) {
                    return depths;
                }
                @Nullable Holder<Biome>[] byRegion = replacements.get(biome);
                if (byRegion == null) {
                    return biome;
                }
                Holder<Biome> replacement = byRegion[region(quartX, quartZ, salt)];
                return replacement != null ? replacement : biome;
            };
        }
    }

    private record Salt(Object noise, long value) {}

    /** The region at a quart column: 0 is vanilla's, 1-4 Xenon's maple, foothills, cold maple and cold birch regions. */
    static int region(int quartX, int quartZ, long salt) {
        int x = quartX + Mth.floor(WARP * valueNoise(quartX, quartZ, salt ^ 0x5DEECE66DL));
        int z = quartZ + Mth.floor(WARP * valueNoise(quartX, quartZ, salt ^ 0x2545F4914F6CDD1DL));
        int pick = (int) Math.floorMod(hash(Math.floorDiv(x, REGION_SIZE), Math.floorDiv(z, REGION_SIZE), salt), TOTAL_WEIGHT);
        for (int region = 0; region < REGION_WEIGHTS.length; region++) {
            pick -= REGION_WEIGHTS[region];
            if (pick < 0) {
                return region;
            }
        }
        return 0;
    }

    /** Smooth noise in [-1, 1]: lattice values every {@link #WARP_SCALE} quarts, blended. */
    private static double valueNoise(int quartX, int quartZ, long salt) {
        int cellX = Math.floorDiv(quartX, WARP_SCALE);
        int cellZ = Math.floorDiv(quartZ, WARP_SCALE);
        double fx = Mth.smoothstep((quartX - cellX * WARP_SCALE) / (float) WARP_SCALE);
        double fz = Mth.smoothstep((quartZ - cellZ * WARP_SCALE) / (float) WARP_SCALE);
        double a = Mth.lerp(fx, lattice(cellX, cellZ, salt), lattice(cellX + 1, cellZ, salt));
        double b = Mth.lerp(fx, lattice(cellX, cellZ + 1, salt), lattice(cellX + 1, cellZ + 1, salt));
        return Mth.lerp(fz, a, b);
    }

    private static double lattice(int x, int z, long salt) {
        return (hash(x, z, salt) >>> 11) * 0x1.0p-53 * 2.0 - 1.0;
    }

    private static long hash(int x, int z, long salt) {
        long h = salt ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
        h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
        return h ^ h >>> 31;
    }
}
