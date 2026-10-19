package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainWoods;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Maple bush — mirrors vanilla jungle bush exactly.
 * One log block at origin, leaves surrounding it at the same level
 * and one layer above. Max 2 blocks tall, always compact.
 * Never generates if a log exists within the footprint.
 */
public record MapleBushFeature() implements Feature {
    public static final MapCodec<MapleBushFeature> CODEC = MapCodec.unit(MapleBushFeature::new);

    @Override
    public MapCodec<MapleBushFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        // Must be on dirt/grass
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK)) return false;

        // Abort if any existing log is within the 3x2x3 footprint
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (world.getBlockState(origin.offset(dx, dy, dz)).is(BlockTags.LOGS)) {
                        return false;
                    }
                }
            }
        }

        Block leaves = random.nextBoolean()
                ? MainWoods.MAPLE.leaves().get(0).get()
                : MainWoods.MAPLE.leaves().get(1).get();

        // Centre log — always exactly 1 log, same as vanilla jungle bush
        world.setBlock(origin,
                MainWoods.MAPLE.log().get().defaultBlockState(),
                Block.UPDATE_ALL);

        // Bottom leaf layer — 3x3 around the log, drop random corners
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                if (corner && random.nextFloat() < 0.5f) continue;
                placeLeaf(world, origin.offset(dx, 0, dz), leaves);
            }
        }

        // Top leaf layer — cross + optional corners, never larger than bottom
        placeLeaf(world, origin.offset(0, 1, 0), leaves);
        for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            if (random.nextFloat() < 0.7f)
                placeLeaf(world, origin.offset(o[0], 1, o[1]), leaves);
        }
        // Corners on top are rare to keep it rounded
        for (int[] o : new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}) {
            if (random.nextFloat() < 0.2f)
                placeLeaf(world, origin.offset(o[0], 1, o[1]), leaves);
        }

        return true;
    }

    private static void placeLeaf(WorldGenLevel world, BlockPos pos, Block leaves) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlock(pos,
                    leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
                    Block.UPDATE_ALL);
        }
    }
}