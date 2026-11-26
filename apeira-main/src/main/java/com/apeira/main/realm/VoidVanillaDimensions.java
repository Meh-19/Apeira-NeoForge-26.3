package com.apeira.main.realm;

import java.util.List;
import java.util.Optional;

import com.apeira.main.registry.MainGameRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * With realm presets on ({@link MainGameRules#REALM_PRESETS}) players live in their realms and their realms' nethers,
 * never in {@code minecraft:overworld} or {@code minecraft:the_nether}, so those generate as void (an empty flat
 * generator over {@code the_void}): {@code mixin/VanillaDimensionsVoidMixin} swaps their generators as the server makes
 * its levels, reading the rule the Create World screen set. Chunks generated before keep their blocks.
 * <p>
 * The overworld still holds the world spawn, where players without a realm wait while the onboarding screen is open:
 * a 3x3 barrier floor is kept under it so nobody falls. Players with a realm who respawn in either vanilla dimension
 * (no bed) are sent to their realm.
 */
public final class VoidVanillaDimensions {
    private static final BlockPos DEFAULT_SPAWN = new BlockPos(0, 64, 0);

    private VoidVanillaDimensions() {}

    /** {@code stem}, or a void one of the same type when {@code key} is the vanilla overworld or nether and realm presets are on. */
    public static LevelStem stem(MinecraftServer server, ResourceKey<LevelStem> key, LevelStem stem) {
        if ((key != LevelStem.OVERWORLD && key != LevelStem.NETHER) || !MainGameRules.realmPresets(server)) {
            return stem;
        }
        Holder<Biome> voidBiome = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_VOID);
        FlatLevelGeneratorSettings flat = new FlatLevelGeneratorSettings(Optional.empty(), voidBiome, List.of());
        return new LevelStem(stem.type(), new FlatLevelSource(flat), stem.seedOverride());
    }

    /** Whether {@code level} is the vanilla overworld or nether. */
    public static boolean isVanilla(ResourceKey<Level> level) {
        return level == Level.OVERWORLD || level == Level.NETHER;
    }

    /** Keeps a floor under the world spawn of a void overworld. */
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!MainGameRules.realmPresets(server)) {
            return;
        }
        ServerLevel overworld = server.overworld();
        LevelData.RespawnData spawn = server.getRespawnData();
        BlockPos pos = spawn.pos();
        if (spawn.dimension() != Level.OVERWORLD || pos.getY() <= overworld.getMinY() + 1) {
            pos = DEFAULT_SPAWN;
            server.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, pos, 0.0F, 0.0F));
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = pos.offset(dx, -1, dz);
                if (overworld.getBlockState(floor).isAir()) {
                    overworld.setBlockAndUpdate(floor, Blocks.BARRIER.defaultBlockState());
                }
            }
        }
    }

    /** Players with a realm who respawn in a vanilla dimension go to their realm instead. */
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isVanilla(player.level().dimension())
                && MainGameRules.realmPresets(player.level().getServer())) {
            MinecraftServer server = player.level().getServer();
            server.execute(() -> {
                if (!player.isRemoved() && isVanilla(player.level().dimension())) {
                    RealmPool.sendHome(player);
                }
            });
        }
    }
}
