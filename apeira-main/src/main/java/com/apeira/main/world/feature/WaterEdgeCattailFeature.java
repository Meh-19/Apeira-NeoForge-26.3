package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scatters cattails on ground that is directly adjacent to water.
 * Handles the two-block tall plant placement manually (lower + upper half)
 * since SimpleBlockFeature only places the lower half without the upper.
 *
 * Targets 1-block-deep water edges — shallow water margins and river banks.
 */
public record WaterEdgeCattailFeature() implements Feature {
    public static final MapCodec<WaterEdgeCattailFeature> CODEC = MapCodec.unit(WaterEdgeCattailFeature::new);

    @Override
    public MapCodec<WaterEdgeCattailFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int placed   = 0;
        int goal     = 3 + random.nextInt(6);   // 3-8 cattails per patch
        int maxTries = goal * 5;

        for (int attempt = 0; attempt < maxTries && placed < goal; attempt++) {
            int dx = random.nextInt(11) - 5;
            int dz = random.nextInt(11) - 5;

            // Find the real surface at this XZ
            BlockPos groundPos = findSurface(world, origin.offset(dx, 0, dz));
            if (groundPos == null) continue;

            // Must be on plantable ground
            BlockState groundState = world.getBlockState(groundPos.below());
            if (!groundState.is(BlockTags.DIRT)
                    && !groundState.is(Blocks.GRAVEL)
                    && !groundState.is(Blocks.SAND)) continue;

            // Both blocks above must be free
            if (!world.getBlockState(groundPos).isAir()) continue;
            if (!world.getBlockState(groundPos.above()).isAir()) continue;

            // Must have water horizontally adjacent (at least one direction)
            boolean nearWater = false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = groundPos.relative(dir);
                if (world.getBlockState(neighbor).is(Blocks.WATER)
                        || world.getBlockState(neighbor.below()).is(Blocks.WATER)) {
                    nearWater = true;
                    break;
                }
            }
            if (!nearWater) continue;

            DoublePlantBlock.placeAt(world, MainPlants.CATTAIL.get().defaultBlockState(), groundPos, Block.UPDATE_CLIENTS);
            placed++;
        }

        return placed > 0;
    }

    /** Scans ±4 blocks in Y to find first air above solid non-water ground. */
    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState below   = world.getBlockState(candidate.below());
            BlockState at      = world.getBlockState(candidate);
            if (!below.isAir() && !below.is(Blocks.WATER) && at.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}