package com.apeira.archae.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Argon's {@code CeilingOnlyFurnitureBlock}: a chandelier or hanging lantern, placed only under a block that can hold
 * it up.
 */
public class CeilingArtifactBlock extends FacingArtifactBlock {
    public CeilingArtifactBlock(BlockBehaviour.Properties properties, boolean paintable) {
        super(properties, paintable, null);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return context.getClickedFace() == Direction.DOWN ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Override
    public DisplayMode displayMode(BlockState state) {
        return DisplayMode.TOP_FACE;
    }
}
