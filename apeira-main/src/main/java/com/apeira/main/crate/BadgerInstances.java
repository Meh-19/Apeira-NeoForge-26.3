package com.apeira.main.crate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Stream;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;
import com.apeira.main.mixin.MinecraftServerAccessor;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Per-player copies of the Badger Crate's void world, {@code apeira:<uuid>_badgercrate}: one dimension per opener, so
 * openings never share blocks, entities or chunks. Each is made from the template (the overworld region and entity
 * files of Ren's {@code run/saves/New World (8)}, shipped in {@code apeira/badger_crate_template/}) copied into the
 * dimension's own folder, then loaded as a void world (flat, no layers, {@code the_void} biome) that only reads the
 * copied chunks, with no weather and always at noon ({@link #holdNoon}). {@link #remove} unloads it and deletes its folder; leftovers from a crash are deleted on server start.
 *
 * <p>Levels are added and removed only between ticks ({@code ServerTickEvent.Post}), never while the server walks its
 * level map.
 */
public final class BadgerInstances {
    private static final String SUFFIX = "_badgercrate";
    private static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ApeiraIds.id("badger_crate"));
    private static final ResourceKey<WorldClock> CLOCK = ResourceKey.create(Registries.WORLD_CLOCK, ApeiraIds.id("badger_crate"));
    /** Six hours past sunrise: noon. */
    private static final long NOON = 6000L;
    private static final String TEMPLATE = "/apeira/badger_crate_template/";
    private static final List<String> REGION_FILES = List.of("r.-1.-1.mca", "r.-1.0.mca", "r.0.-1.mca", "r.0.0.mca");
    private static final List<String> ENTITY_FILES = List.of("r.0.-1.mca");

    private BadgerInstances() {}

    /** {@code apeira:<uuid>_badgercrate}. */
    public static ResourceKey<Level> key(UUID player) {
        return ResourceKey.create(Registries.DIMENSION, ApeiraIds.id(player.toString().toLowerCase(Locale.ROOT) + SUFFIX));
    }

    public static boolean isInstance(ResourceKey<Level> key) {
        return key.identifier().getNamespace().equals(Apeira.MOD_ID) && key.identifier().getPath().endsWith(SUFFIX);
    }

    /** Copies the template and loads the player's instance, replacing any old one. */
    public static ServerLevel create(MinecraftServer server, UUID player) throws IOException {
        ResourceKey<Level> key = key(player);
        remove(server, key);
        MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
        Path folder = accessor.apeira$storageSource().getDimensionPath(key);
        copyTemplate(folder.resolve("region"), REGION_FILES);
        copyTemplate(folder.resolve("entities"), ENTITY_FILES);

        Holder<DimensionType> type = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(DIMENSION_TYPE);
        holdNoon(server);
        Holder<Biome> voidBiome = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_VOID);
        FlatLevelGeneratorSettings flat = new FlatLevelGeneratorSettings(Optional.empty(), voidBiome, List.of());
        LevelStem stem = new LevelStem(type, new FlatLevelSource(flat), OptionalLong.of(0L));
        ServerLevel level = new ServerLevel(server, accessor.apeira$executor(), accessor.apeira$storageSource(),
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()), key, stem,
                false, BiomeManager.obfuscateSeed(0L), List.of(), false) {
            /** No rain or thunder, whatever the overworld's weather. */
            @Override
            public boolean canHaveWeather() {
                return false;
            }
        };
        accessor.apeira$levels().put(key, level);
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }

    /**
     * Instances use {@code apeira:badger_crate}, the overworld's dimension type with its own clock
     * ({@code apeira:badger_crate}), which is kept paused at noon so it is always day.
     */
    private static void holdNoon(MinecraftServer server) {
        Holder<WorldClock> clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(CLOCK);
        server.clockManager().setTotalTicks(clock, NOON);
        server.clockManager().setPaused(clock, true);
    }

    /** Unloads the instance, if loaded, and deletes its folder. Players still in it must have been moved out first. */
    public static void remove(MinecraftServer server, ResourceKey<Level> key) {
        MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
        ServerLevel level = accessor.apeira$levels().remove(key);
        if (level != null) {
            NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
            try {
                level.close();
            } catch (IOException e) {
                Apeira.LOGGER.warn("Could not close Badger Crate instance {}", key.identifier(), e);
            }
        }
        delete(accessor.apeira$storageSource().getDimensionPath(key));
    }

    /** Deletes every instance folder left by a server that stopped mid-opening. */
    public static void deleteLeftovers(MinecraftServer server) {
        Path apeiraDimensions = ((MinecraftServerAccessor) server).apeira$storageSource().getDimensionPath(key(new UUID(0, 0))).getParent();
        if (!Files.isDirectory(apeiraDimensions)) {
            return;
        }
        try (Stream<Path> folders = Files.list(apeiraDimensions)) {
            folders.filter(path -> path.getFileName().toString().endsWith(SUFFIX)).forEach(BadgerInstances::delete);
        } catch (IOException e) {
            Apeira.LOGGER.warn("Could not list Badger Crate instances", e);
        }
    }

    private static void copyTemplate(Path target, List<String> files) throws IOException {
        Files.createDirectories(target);
        for (String file : files) {
            String resource = TEMPLATE + target.getFileName() + "/" + file;
            try (InputStream in = BadgerInstances.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("missing " + resource);
                }
                Files.copy(in, target.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void delete(Path folder) {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    Apeira.LOGGER.warn("Could not delete {}", path, e);
                }
            });
        } catch (IOException e) {
            Apeira.LOGGER.warn("Could not delete Badger Crate instance {}", folder, e);
        }
    }
}
