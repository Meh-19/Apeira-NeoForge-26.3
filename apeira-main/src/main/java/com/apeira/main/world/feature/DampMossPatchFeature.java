package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainStones;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scatters 1–4 damp_moss blocks on top of packed_dirt or dried_silt
 * surface blocks within a small radius of the origin.
 * Used in the Foothills biome to add ground texture to dry patches.
 */
public record DampMossPatchFeature() implements Feature {
    public static final MapCodec<DampMossPatchFeature> CODEC = MapCodec.unit(DampMossPatchFeature::new);

    @Override
    public MapCodec<DampMossPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int goal     = 1 + random.nextInt(4);   // 1-4 pieces
        int radius   = 2 + random.nextInt(2);   // search radius 2-3
        int placed   = 0;
        int maxTries = goal * 5;

        for (int attempt = 0; attempt < maxTries && placed < goal; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;

            BlockPos surface = findValidSurface(world, origin.offset(dx, 0, dz));
            if (surface == null) continue;

            BlockPos above = surface.above();
            if (!world.getBlockState(above).isAir()) continue;

            world.setBlock(above,
                    MainPlants.DAMP_MOSS.get().defaultBlockState(),
                    Block.UPDATE_ALL);
            placed++;
        }

        return placed > 0;
    }

    /**
     * Scans ±3 Y to find a packed_dirt or dried_silt block with air above it.
     * Returns the surface block position.
     */
    private static BlockPos findValidSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            if ((state.is(MainStones.PACKED_DIRT.get()) || state.is(MainStones.DRIED_SILT.get()))
                    && world.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return null;
    }
}