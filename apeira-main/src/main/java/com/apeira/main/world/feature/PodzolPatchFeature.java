package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

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
 * Places an irregular podzol patch by replacing grass/dirt surface blocks.
 * Radius 2–4, slightly elliptical with noise on the edge so it never looks like
 * a perfect circle.
 */
public record PodzolPatchFeature() implements Feature {
    public static final MapCodec<PodzolPatchFeature> CODEC = MapCodec.unit(PodzolPatchFeature::new);

    @Override
    public MapCodec<PodzolPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int radius  = 5 + random.nextInt(5); // 5-9
        int placed  = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Irregular circular shape
                float noiseEdge = radius - 0.4f + random.nextFloat() * 0.9f;
                if (dx * dx + dz * dz > noiseEdge * noiseEdge) continue;

                BlockPos surface = findSurface(world, origin.offset(dx, 0, dz));
                if (surface == null) continue;

                BlockState current = world.getBlockState(surface);
                if (!current.is(BlockTags.DIRT) && !current.is(Blocks.GRASS_BLOCK)) continue;

                world.setBlock(surface, Blocks.PODZOL.defaultBlockState(), Block.UPDATE_ALL);
                placed++;
            }
        }

        return placed > 0;
    }

    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            BlockState above   = world.getBlockState(candidate.above());
            if ((state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK))
                    && above.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}