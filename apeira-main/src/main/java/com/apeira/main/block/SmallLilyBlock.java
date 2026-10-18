package com.apeira.main.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Xenon's small lily: a lily pad (placed on still water or ice) that boats pass through without breaking it. */
public class SmallLilyBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1.5, 15);

    public SmallLilyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return (level.getFluidState(pos).is(Fluids.WATER) || state.getBlock() instanceof IceBlock) && level.getFluidState(pos.above()).isEmpty();
    }
}
