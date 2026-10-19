package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places a horizontal oak log (3-5 blocks long) on flat ground.
 * Shelf fungus spawns on top of log segments (~25% chance per segment).
 */
public record FallenLogFeature() implements Feature {
    public static final MapCodec<FallenLogFeature> CODEC = MapCodec.unit(FallenLogFeature::new);

    @Override
    public MapCodec<FallenLogFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT)
                && !ground.is(Blocks.GRASS_BLOCK)
                && !ground.is(Blocks.MOSS_BLOCK)
                && !ground.is(Blocks.GRAVEL)) {
            return false;
        }

        int logY = origin.getY();

        Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        BlockState logState = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);

        int length      = 3 + random.nextInt(3);
        int startOffset = -(length / 2) + random.nextInt(2);

        // Flat-ground pre-check — all segments must be at same Y with solid ground below
        for (int i = 0; i < length; i++) {
            int offset = startOffset + i;
            BlockPos segPos = axis == Direction.Axis.X
                    ? new BlockPos(origin.getX() + offset, logY, origin.getZ())
                    : new BlockPos(origin.getX(), logY, origin.getZ() + offset);

            BlockState below = world.getBlockState(segPos.below());
            BlockState at    = world.getBlockState(segPos);

            if (below.isAir() || below.is(Blocks.WATER) || below.canBeReplaced()) return false;
            if (!at.isAir() && !at.is(BlockTags.REPLACEABLE)) return false;
        }

        int placed = 0;
        for (int i = 0; i < length; i++) {
            int offset = startOffset + i;
            BlockPos segPos = axis == Direction.Axis.X
                    ? new BlockPos(origin.getX() + offset, logY, origin.getZ())
                    : new BlockPos(origin.getX(), logY, origin.getZ() + offset);

            world.setBlock(segPos, logState, Block.UPDATE_ALL);
            placed++;

            // Shelf fungus on top of log — ~25% chance per segment
            if (random.nextFloat() < 0.25f) {
                BlockPos above = segPos.above();
                if (world.getBlockState(above).isAir()) {
                    world.setBlock(above,
                            MainPlants.SHELF_FUNGUS.get().defaultBlockState()
                                    .setValue(RandomRotationBlock.ROTATION, random.nextInt(4)),
                            Block.UPDATE_ALL);
                }
            }
        }

        return placed > 0;
    }
}