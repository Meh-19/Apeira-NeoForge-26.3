package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

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
 * Large oak bush — oak log at centre, irregular 5×5 leaf skirt,
 * 3×3 cap one block above, small cross tip two blocks above.
 * Never generates if another log is within the 5×5×3 footprint.
 */
public record OakBushFeature() implements Feature {
    public static final MapCodec<OakBushFeature> CODEC = MapCodec.unit(OakBushFeature::new);

    // 5×5 leaf ring at y=0 — same shape as FirBushFeature
    private static final int[][] SKIRT = {
            {-2,-1},{-2,0},{-2,1},
            {-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            { 0,-2},{ 0,-1},        { 0,1},{ 0,2},
            { 1,-2},{ 1,-1},{ 1,0},{ 1,1},{ 1,2},
            { 2,-1},{ 2,0},{ 2,1}
    };

    // 3×3 cap at y=1
    private static final int[][] CAP = {
            {-1,-1},{-1,0},{-1,1},
            { 0,-1},{ 0,0},{ 0,1},
            { 1,-1},{ 1,0},{ 1,1}
    };

    // Cross tip at y=2
    private static final int[][] TIP = {
            {-1,0},{0,-1},{0,0},{0,1},{1,0}
    };

    @Override
    public MapCodec<OakBushFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK)
                && !ground.is(Blocks.PODZOL)) return false;

        // Abort if any log in 5×5×3 footprint
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (world.getBlockState(origin.offset(dx, dy, dz)).is(BlockTags.LOGS)) return false;
                }
            }
        }

        Block leaves = Blocks.OAK_LEAVES;

        // Central oak log
        world.setBlock(origin, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);

        for (int[] o : SKIRT) placeLeaf(world, origin.offset(o[0], 0, o[1]), leaves);
        for (int[] o : CAP)   placeLeaf(world, origin.offset(o[0], 1, o[1]), leaves);
        for (int[] o : TIP)   placeLeaf(world, origin.offset(o[0], 2, o[1]), leaves);

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