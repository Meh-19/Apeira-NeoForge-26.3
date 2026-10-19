package com.apeira.main.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.world.DepthsTerrain;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

/**
 * Builds Xenon's Depths ({@link DepthsTerrain}) under each chunk once its surface rules have run, before the carvers, as
 * Xenon's mixin did after the surface step. 26.x builds noise, surface and carvers in one step, so this is the private
 * surface part of it.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class DepthsTerrainMixin {
    @SuppressWarnings("deprecation")
    @Inject(method = "buildSurface", at = @At("TAIL"))
    private void apeira$buildDepths(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState, BiomeManager biomeManager,
            Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule, CallbackInfo ci) {
        DepthsTerrain.forSeed(randomState.seed()).generate(chunk);
    }
}
