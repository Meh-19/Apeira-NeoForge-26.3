package com.apeira.main.mixin;

import java.util.Map;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

/** What {@link com.apeira.main.realm.RealmLevels} needs to add a dimension while the server runs. */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> apeira$levels();

    @Accessor("executor")
    Executor apeira$executor();

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess apeira$storageSource();
}
