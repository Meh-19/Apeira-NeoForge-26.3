package com.apeira.main.realm;

import java.util.List;
import java.util.OptionalLong;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.mixin.MinecraftServerAccessor;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Makes realm dimensions while the server runs. A realm is the overworld's dimension type and terrain noise with its
 * preset's biome source ({@link RealmPreset#biomeSource}) and a seed of its own (NeoForge's {@link LevelStem} seed
 * override), inside a vanilla world border {@link #BORDER_SIZE} blocks wide centred on 0, 0. Its time and weather
 * follow the overworld's, as the other vanilla dimensions'.
 * <p>
 * Each realm has a nether of its own, made with it ({@link #netherKey}, {@code apeira:realm/<id>_nether}): vanilla's
 * nether terrain and biomes with the realm's seed, in the same border, as the dimension type {@code apeira:realm_nether}
 * (the nether's with a coordinate scale of 1, so portals link 1:1). {@code mixin/RealmPortalMixin} lights portals in
 * both and links them to each other instead of to the vanilla overworld and nether.
 */
public final class RealmLevels {
    public static final int BORDER_SIZE = 300;
    /** Seeds tried for one whose border area is land; the last is used whatever it is. */
    private static final int SEED_ATTEMPTS = 32;
    /** Continentalness above this is inland; vanilla's coasts start at about -0.11. */
    private static final float INLAND = 0.03F;
    /** The nether's dimension type with a coordinate scale of 1 ({@code data/apeira/dimension_type/realm_nether.json}). */
    public static final ResourceKey<DimensionType> NETHER_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ApeiraIds.id("realm_nether"));
    private static final String REALM_PREFIX = "realm/";
    private static final String NETHER_SUFFIX = "_nether";

    private RealmLevels() {}

    /** Creates and registers the level for {@code realm}, or returns it if the server already has it. */
    public static ServerLevel create(MinecraftServer server, RealmData.Realm realm) {
        ServerLevel existing = server.getLevel(realm.levelKey());
        if (existing != null) {
            return existing;
        }
        RegistryAccess registries = server.registryAccess();
        Holder<DimensionType> type = registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD);
        LevelStem stem = new LevelStem(type,
                new NoiseBasedChunkGenerator(realm.preset().biomeSource(registries.lookupOrThrow(Registries.BIOME)), noiseSettings(server)),
                OptionalLong.of(realm.seed()));
        MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
        ServerLevel level = new ServerLevel(server, accessor.apeira$executor(), accessor.apeira$storageSource(),
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()), realm.levelKey(), stem,
                server.getWorldData().isDebugWorld(), BiomeManager.obfuscateSeed(realm.seed()), List.of(), false);
        add(server, realm.levelKey(), level);
        if (server.getLevel(netherKey(realm)) == null) {
            Holder<DimensionType> netherType = registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(NETHER_TYPE);
            MultiNoiseBiomeSource netherBiomes = MultiNoiseBiomeSource.createFromPreset(registries
                    .lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER));
            Holder<NoiseGeneratorSettings> netherNoise = registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.NETHER);
            LevelStem netherStem = new LevelStem(netherType, new NoiseBasedChunkGenerator(netherBiomes, netherNoise), OptionalLong.of(realm.seed()));
            add(server, netherKey(realm), new ServerLevel(server, accessor.apeira$executor(), accessor.apeira$storageSource(),
                    new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()), netherKey(realm), netherStem,
                    server.getWorldData().isDebugWorld(), BiomeManager.obfuscateSeed(realm.seed()), List.of(), false));
        }
        return level;
    }

    private static void add(MinecraftServer server, ResourceKey<Level> key, ServerLevel level) {
        ((MinecraftServerAccessor) server).apeira$levels().put(key, level);
        WorldBorder border = level.getWorldBorder();
        border.setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
        border.setCenter(0.0, 0.0);
        border.setSize(BORDER_SIZE);
        server.getPlayerList().addWorldborderListener(level);
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }

    /** The level key of {@code realm}'s nether. */
    public static ResourceKey<Level> netherKey(RealmData.Realm realm) {
        return ResourceKey.create(Registries.DIMENSION, netherId(realm.dimension()));
    }

    /** The id of the nether of the realm whose overworld is {@code overworld}. */
    public static Identifier netherId(Identifier overworld) {
        return Identifier.fromNamespaceAndPath(overworld.getNamespace(), overworld.getPath() + NETHER_SUFFIX);
    }

    /** {@code dimension} itself, or for a realm's nether the realm's overworld. */
    public static Identifier overworldId(Identifier dimension) {
        return isRealmNether(dimension)
                ? Identifier.fromNamespaceAndPath(dimension.getNamespace(), dimension.getPath().substring(0, dimension.getPath().length() - NETHER_SUFFIX.length()))
                : dimension;
    }

    /** Whether {@code dimension} is a realm's overworld or nether, by its id alone (both sides know it). */
    public static boolean isRealm(ResourceKey<Level> dimension) {
        Identifier id = dimension.identifier();
        return id.getNamespace().equals(ApeiraIds.NAMESPACE) && id.getPath().startsWith(REALM_PREFIX);
    }

    public static boolean isRealmNether(ResourceKey<Level> dimension) {
        return isRealmNether(dimension.identifier());
    }

    private static boolean isRealmNether(Identifier id) {
        return id.getNamespace().equals(ApeiraIds.NAMESPACE) && id.getPath().startsWith(REALM_PREFIX) && id.getPath().endsWith(NETHER_SUFFIX);
    }

    /**
     * Where a nether portal in the realm level {@code level} leads: a realm's overworld to its nether and back. Null
     * when that level is not loaded.
     */
    public static @Nullable ServerLevel portalTarget(ServerLevel level) {
        Identifier id = level.dimension().identifier();
        Identifier target = isRealmNether(id) ? overworldId(id) : netherId(id);
        return level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, target));
    }

    /** A seed whose terrain is land at the centre and the corners of the border, so a realm is not an ocean. */
    public static long landSeed(MinecraftServer server, RandomSource random) {
        NoiseGeneratorSettings settings = noiseSettings(server).value();
        HolderGetter<NormalNoise> noises = server.registryAccess().lookupOrThrow(Registries.NOISE);
        int edge = BORDER_SIZE / 2 - 16;
        long seed = random.nextLong();
        for (int attempt = 0; attempt < SEED_ATTEMPTS; attempt++) {
            RandomState state = RandomState.create(noises, seed, settings);
            if (isInland(state, settings, 0, 0) && isInland(state, settings, edge, edge) && isInland(state, settings, -edge, edge)
                    && isInland(state, settings, edge, -edge) && isInland(state, settings, -edge, -edge)) {
                return seed;
            }
            seed = random.nextLong();
        }
        return seed;
    }

    private static boolean isInland(RandomState state, NoiseGeneratorSettings settings, int x, int z) {
        return state.sampleBlockValueUncached(settings.noiseRouter().continents(), x, 0, z) > INLAND;
    }

    private static Holder<NoiseGeneratorSettings> noiseSettings(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
    }
}
