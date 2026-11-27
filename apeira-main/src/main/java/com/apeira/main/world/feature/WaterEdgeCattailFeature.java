package com.apeira.main.world.feature;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A clump of cattails where land meets water: on the bank right beside water, or standing in water exactly one block
 * deep (the lower half waterlogged). Each try picks a column within {@link #RADIUS} of the origin and finds its ground
 * near the origin's height; short grass and other replaceable plants on a bank are replaced, as vanilla's tall grass
 * patches do, so grassy banks get cattails too. Places nothing when the origin is not near water.
 */
public record WaterEdgeCattailFeature() implements Feature {
    public static final MapCodec<WaterEdgeCattailFeature> CODEC = MapCodec.unit(WaterEdgeCattailFeature::new);
    private static final int RADIUS = 7;

    @Override
    public MapCodec<WaterEdgeCattailFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int goal = 4 + random.nextInt(7);
        int placed = 0;
        for (int attempt = 0; attempt < 96 && placed < goal; attempt++) {
            BlockPos column = origin.offset(random.nextInt(RADIUS * 2 + 1) - RADIUS, 0, random.nextInt(RADIUS * 2 + 1) - RADIUS);
            if (tryPlace(world, column)) {
                placed++;
            }
        }
        return placed > 0;
    }

    /**
     * Places one cattail in {@code column}'s x and z, looking for ground from 4 blocks above to 4 below its y, when that
     * spot is a bank beside water or water one block deep.
     */
    public static boolean tryPlace(WorldGenLevel world, BlockPos column) {
        BlockPos pos = cattailSpot(world, column);
        if (pos == null) {
            return false;
        }
        BlockState cattail = MainPlants.CATTAIL.get().defaultBlockState();
        if (!cattail.canSurvive(world, pos)) {
            return false;
        }
        DoublePlantBlock.placeAt(world, cattail, pos, Block.UPDATE_CLIENTS);
        return true;
    }

    private static @Nullable BlockPos cattailSpot(WorldGenLevel world, BlockPos column) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos pos = column.above(dy);
            BlockState below = world.getBlockState(pos.below());
            if (below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced()) {
                continue;
            }
            BlockState at = world.getBlockState(pos);
            if (!isFree(world, pos.above())) {
                return null;
            }
            if (at.is(Blocks.WATER)) {
                // Standing in water: exactly one block of still water over the ground.
                return world.getFluidState(pos).isSource() ? pos : null;
            }
            if (isFree(world, pos) && besideWater(world, pos)) {
                return pos;
            }
            return null;
        }
        return null;
    }

    /** Air, or a dry plant that may be replaced (short grass, ferns). */
    private static boolean isFree(WorldGenLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() && !state.is(MainPlants.CATTAIL.get());
    }

    /** Water beside {@code pos}, level with it or a block lower (a bank above the water line). */
    private static boolean besideWater(WorldGenLevel world, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (world.getFluidState(side).is(FluidTags.WATER) || world.getFluidState(side.below()).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
