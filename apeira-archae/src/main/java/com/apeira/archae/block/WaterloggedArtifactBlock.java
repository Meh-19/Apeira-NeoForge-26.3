package com.apeira.archae.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Argon's {@code LostTreasureChestBlock} and {@code WhaleStatueBlock}: a waterloggable artifact with a second model for
 * under water. Placed in water, the dry artifact places its water variant instead, which drops the dry one.
 */
public class WaterloggedArtifactBlock extends FacingArtifactBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final DisplayMode mode;
    private final @Nullable Supplier<? extends Block> water;

    /**
     * @param drop  the dry artifact, for the water variant; {@code null} for the dry artifact itself
     * @param water the water variant, for the dry artifact; {@code null} for the water variant itself
     */
    public WaterloggedArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, DisplayMode mode,
            @Nullable Supplier<? extends Block> drop, @Nullable Supplier<? extends Block> water) {
        super(properties, paintable, drop);
        this.mode = mode;
        this.water = water;
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        if (inWater && water != null) {
            return water.get().defaultBlockState().setValue(FACING, state.getValue(FACING)).setValue(ROTATION, state.getValue(ROTATION))
                    .setValue(WATERLOGGED, true);
        }
        return state.setValue(WATERLOGGED, inWater);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public DisplayMode displayMode(BlockState state) {
        return mode;
    }
}
