package com.apeira.main.realm.balloon;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The realm's starter balloon, the centre of its 3x3 basket floor: Ren's GeckoLib model drawn by its block entity
 * ({@link StarterBalloonBlockEntity}), torch-bright (light 14) and unbreakable. The eight cells around it are vanilla
 * barriers ({@link StarterBalloon#placeBlocks}), so the whole floor can be stood on; players spawn on it. Only
 * {@link StarterBalloon} places and moves it, when the realm is made and through {@code /realm setspawn}. Its
 * {@link #FACING} is the realm's spawn facing, the way its front (north in the model) and arriving players look.
 */
public class StarterBalloonBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public StarterBalloonBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StarterBalloonBlockEntity(pos, state);
    }

    /** A balloon removed any other way (creative breaking, commands) takes its barrier floor with it. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        StarterBalloon.removeParts(level, pos);
    }
}
