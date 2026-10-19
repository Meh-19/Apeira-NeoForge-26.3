package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scatters fallen sticks on the ground in a small irregular patch.
 * Only places on dirt, grass, podzol, or moss — never floats.
 */
public record StickPatchFeature() implements Feature {
    public static final MapCodec<StickPatchFeature> CODEC = MapCodec.unit(StickPatchFeature::new);

    @Override
    public MapCodec<StickPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int radius = 2 + random.nextInt(3); // 2-4
        int tries  = 3 + random.nextInt(5); // 3-7 sticks per patch
        int placed = 0;

        for (int attempt = 0; attempt < tries * 3 && placed < tries; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) continue;

            BlockPos surface = findSurface(world, origin.offset(dx, 0, dz));
            if (surface == null) continue;

            BlockPos placePos = surface.above();
            if (!world.getBlockState(placePos).isAir()) continue;

            world.setBlock(placePos,
                    MainPlants.TWIG.get().defaultBlockState(),
                    Block.UPDATE_ALL);
            placed++;
        }

        return placed > 0;
    }

    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            BlockState above   = world.getBlockState(candidate.above());
            if ((state.is(BlockTags.DIRT)
                    || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.PODZOL)
                    || state.is(Blocks.MOSS_BLOCK)
                    || state.is(Blocks.COARSE_DIRT))
                    && above.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}