package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scatters cold leaf litter (dead spruce needle debris) on the ground.
 * Places on grass, podzol, and dirt — biased toward podzol patches
 * that form under spruce trees.
 * Falls back to DRIED_LEAF_LITTER if COLD_LEAF_LITTER doesn't exist yet.
 */
public record ColdLeafLitterFeature() implements Feature {
    public static final MapCodec<ColdLeafLitterFeature> CODEC = MapCodec.unit(ColdLeafLitterFeature::new);

    @Override
    public MapCodec<ColdLeafLitterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int radius = 2 + random.nextInt(4); // 2-5 — slightly larger than warm litter
        int tries  = 4 + random.nextInt(7); // 4-10 pieces per patch
        int placed = 0;

        for (int attempt = 0; attempt < tries * 3 && placed < tries; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) continue;

            BlockPos surface = findValidSurface(world, origin.offset(dx, 0, dz));
            if (surface == null) continue;

            BlockPos placePos = surface.above();
            if (!world.getBlockState(placePos).isAir()) continue;

            world.setBlock(placePos,
                    MainPlants.COLD_LEAF_LITTER.get().defaultBlockState()
                            .setValue(RandomRotationBlock.ROTATION, random.nextInt(4)),
                    Block.UPDATE_ALL);
            placed++;
        }

        return placed > 0;
    }

    /**
     * Finds a grass, podzol, or dirt surface with air above it.
     * Biased toward podzol so litter clusters under spruce trees naturally.
     */
    private static BlockPos findValidSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            BlockState above   = world.getBlockState(candidate.above());
            if ((state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.PODZOL)
                    || state.is(Blocks.DIRT)
                    || state.is(Blocks.COARSE_DIRT))
                    && above.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}