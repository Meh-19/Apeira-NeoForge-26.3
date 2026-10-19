package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import java.util.HashSet;
import java.util.Set;

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
 * Places a small irregular stone surface exposure, ringed by coarse dirt.
 * Mimics a mini stone-shore patch — the kind you see where bedrock/stone
 * juts up through the soil in hilly terrain.
 *
 * Stone radius:       2–4 blocks (irregular blob)
 * Coarse dirt fringe: 1–2 blocks beyond the stone edge
 */
public record StoneShoreFeature() implements Feature {
    public static final MapCodec<StoneShoreFeature> CODEC = MapCodec.unit(StoneShoreFeature::new);

    @Override
    public MapCodec<StoneShoreFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int stoneRadius = 2 + random.nextInt(3); // 2-4

        Set<BlockPos> stonePlaced = new HashSet<>();

        // ── Pass 1: stone blob ────────────────────────────────────────────────
        for (int dx = -stoneRadius; dx <= stoneRadius; dx++) {
            for (int dz = -stoneRadius; dz <= stoneRadius; dz++) {
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                // Irregular edge — noise based on dx/dz parity
                float noiseEdge = stoneRadius - 0.5f + (random.nextFloat() * 1.2f);
                if (dist > noiseEdge) continue;

                BlockPos surface = findReplaceable(world, origin.offset(dx, 0, dz));
                if (surface == null) continue;

                world.setBlock(surface, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                stonePlaced.add(surface);
            }
        }

        if (stonePlaced.isEmpty()) return false;

        // ── Pass 2: coarse dirt fringe ────────────────────────────────────────
        int fringeRadius = stoneRadius + 2;
        for (int dx = -fringeRadius; dx <= fringeRadius; dx++) {
            for (int dz = -fringeRadius; dz <= fringeRadius; dz++) {
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                if (dist <= stoneRadius || dist > fringeRadius) continue;

                BlockPos surface = findReplaceable(world, origin.offset(dx, 0, dz));
                if (surface == null) continue;
                if (stonePlaced.contains(surface)) continue; // don't clobber stone

                // Only replace dirt/grass — don't clobber water or other stone
                BlockState current = world.getBlockState(surface);
                if (!current.is(BlockTags.DIRT) && !current.is(Blocks.GRASS_BLOCK)) continue;

                // Fade probability toward the outer edge
                float prob = 1.0f - ((dist - stoneRadius) / (fringeRadius - stoneRadius));
                if (random.nextFloat() < prob) {
                    world.setBlock(surface, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        return true;
    }

    /**
     * Finds the topmost replaceable surface block (grass/dirt/stone/gravel)
     * within ±3 Y of the target, ignoring water.
     */
    private static BlockPos findReplaceable(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            BlockState above   = world.getBlockState(candidate.above());
            boolean solid = state.is(BlockTags.DIRT)
                    || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.STONE)
                    || state.is(Blocks.GRAVEL)
                    || state.is(Blocks.COARSE_DIRT);
            if (solid && (above.isAir() || above.canBeReplaced())) {
                return candidate;
            }
        }
        return null;
    }
}