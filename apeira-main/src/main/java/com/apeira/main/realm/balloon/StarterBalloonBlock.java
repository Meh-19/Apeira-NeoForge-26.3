package com.apeira.main.realm.balloon;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The realm's starter balloon, the centre of its 3x3 basket floor: Ren's GeckoLib model drawn by its block entity
 * ({@link StarterBalloonBlockEntity}), torch-bright (light 14) and unbreakable. The eight cells around it are
 * {@link StarterBalloonPartBlock}s, so the whole floor can be stood on; players spawn on it. Only {@link StarterBalloon}
 * places and moves it, when the realm is made and through {@code /realm setspawn}.
 */
public class StarterBalloonBlock extends BaseEntityBlock {
    public StarterBalloonBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StarterBalloonBlockEntity(pos, state);
    }

    /** A balloon removed any other way (creative breaking, commands) takes its basket floor with it. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        StarterBalloon.removeParts(level, pos);
    }
}
