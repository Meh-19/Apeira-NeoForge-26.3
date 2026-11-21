package com.apeira.decor.furniture;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An animated furniture piece that hangs only under a block, such as the Business Crate's office ceiling fan: placed by
 * clicking the bottom of a block, turned like any piece by {@link FurnitureBlock#ROTATION}, and gone when the block
 * holding it up is.
 */
public class AnimatedCeilingFurnitureBlock extends AnimatedFurnitureBlock {
    public AnimatedCeilingFurnitureBlock(BlockBehaviour.Properties properties, String folder, boolean paintable, String animation) {
        super(properties, folder, paintable, animation, null);
    }

    /** Only under a block. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return context.getClickedFace() == Direction.DOWN ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.above(), Direction.DOWN);
    }
}
