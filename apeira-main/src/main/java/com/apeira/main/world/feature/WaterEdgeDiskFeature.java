package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainStones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places small irregular silt patches only on ground that is directly adjacent
 * to water — rivers, ponds, lake shores.  Silt never spawns inland.
 *
 * Uses MapleTreeFeatureConfig (empty) as its config type for simplicity.
 */
public record WaterEdgeDiskFeature() implements Feature {
    public static final MapCodec<WaterEdgeDiskFeature> CODEC = MapCodec.unit(WaterEdgeDiskFeature::new);

    @Override
    public MapCodec<WaterEdgeDiskFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        // Only proceed if the origin itself is near water
        if (!isNearWater(world, origin)) return false;

        int radius  = 2 + random.nextInt(3);   // patch radius 2-4
        int placed  = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue; // circular

                BlockPos surface = findSurface(world, origin.offset(dx, 0, dz));
                if (surface == null) continue;

                // The block below the air must be replaceable dirt/grass/gravel/sand
                BlockState below = world.getBlockState(surface.below());
                if (!isReplaceable(below)) continue;

                // Must be near water (at surface level or one below)
                if (!isNearWater(world, surface)) continue;

                // Replace the surface block with silt (below the air position)
                world.setBlock(surface.below(),
                        MainStones.SILT.get().defaultBlockState(),
                        Block.UPDATE_ALL);
                placed++;
            }
        }

        return placed > 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Scans ±4 Y to find the first air block above solid non-water ground. */
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

    /** Returns true if any horizontally adjacent block (at pos or pos.down) is water. */
    private static boolean isNearWater(WorldGenLevel world, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (world.getBlockState(neighbor).is(Blocks.WATER)
                    || world.getBlockState(neighbor.below()).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true for blocks that silt can replace at the surface. */
    private static boolean isReplaceable(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.COARSE_DIRT);
    }
}