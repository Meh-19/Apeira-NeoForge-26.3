package com.apeira.main.realm;

import java.util.List;
import java.util.OptionalLong;

import com.apeira.main.mixin.MinecraftServerAccessor;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
 */
public final class RealmLevels {
    public static final int BORDER_SIZE = 300;
    /** Seeds tried for one whose border area is land; the last is used whatever it is. */
    private static final int SEED_ATTEMPTS = 32;
    /** Continentalness above this is inland; vanilla's coasts start at about -0.11. */
    private static final float INLAND = 0.03F;

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
        accessor.apeira$levels().put(realm.levelKey(), level);
        WorldBorder border = level.getWorldBorder();
        border.setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
        border.setCenter(0.0, 0.0);
        border.setSize(BORDER_SIZE);
        server.getPlayerList().addWorldborderListener(level);
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
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
