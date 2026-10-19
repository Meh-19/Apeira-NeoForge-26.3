package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainStones;

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
 * Places silt patches on the floor of water bodies, and extends slightly
 * up onto the adjacent bank (1–2 blocks above the waterline).
 *
 * Placement: use OCEAN_FLOOR_WG heightmap so the origin lands on the
 * underwater floor. The patch then crawls upward onto the bank.
 */
public record UnderwaterSiltPatchFeature() implements Feature {
    public static final MapCodec<UnderwaterSiltPatchFeature> CODEC = MapCodec.unit(UnderwaterSiltPatchFeature::new);

    @Override
    public MapCodec<UnderwaterSiltPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int radius  = 2 + random.nextInt(3); // 2-4
        int placed  = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;

                // ── Underwater floor ─────────────────────────────────────────
                // Walk down from origin Y to find the actual floor
                BlockPos floor = findUnderWaterFloor(world, origin.offset(dx, 0, dz));
                if (floor != null && isReplaceableFloor(world.getBlockState(floor))) {
                    world.setBlock(floor, MainStones.SILT.get().defaultBlockState(), Block.UPDATE_ALL);
                    placed++;
                }

                // ── Bank extension (up to 2 blocks above waterline) ──────────
                // Find the first non-water surface block
                BlockPos bank = findBankSurface(world, origin.offset(dx, 0, dz));
                if (bank != null && random.nextFloat() < 0.4f
                        && isReplaceableFloor(world.getBlockState(bank))) {
                    world.setBlock(bank, MainStones.SILT.get().defaultBlockState(), Block.UPDATE_ALL);
                    placed++;
                }
            }
        }

        return placed > 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Scan downward from the given XZ to find a solid floor block that is
     * below at least one layer of water.
     */
    private static BlockPos findUnderWaterFloor(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -8; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state = world.getBlockState(candidate);
            BlockState above = world.getBlockState(candidate.above());
            if (!state.is(Blocks.WATER) && !state.isAir()
                    && (above.is(Blocks.WATER))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Scan for the surface block just above the water edge (0–2 blocks above
     * waterline), suitable for a silt bank extension.
     */
    private static BlockPos findBankSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state = world.getBlockState(candidate);
            BlockState above = world.getBlockState(candidate.above());
            // Solid surface with air above, NOT below water
            if (!state.isAir() && !state.is(Blocks.WATER)
                    && (above.isAir() || above.canBeReplaced())) {
                // Must be within 2 blocks of a water column nearby
                if (isNearWater(world, candidate)) return candidate;
            }
        }
        return null;
    }

    private static boolean isNearWater(WorldGenLevel world, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (world.getBlockState(pos.offset(dx, dy, dz)).is(Blocks.WATER)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isReplaceableFloor(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.CLAY)
                || state.is(MainStones.SILT.get());
    }
}