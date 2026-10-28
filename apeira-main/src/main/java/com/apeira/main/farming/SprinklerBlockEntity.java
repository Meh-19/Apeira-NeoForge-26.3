package com.apeira.main.farming;

import com.apeira.main.registry.MainFarming;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A sprinkler's block entity. It keeps nothing (the water is in the {@link FarmClock}) and never ticks: it only shows
 * the sprinkler's water level when its chunk loads, and unregisters the sprinkler when it is removed.
 */
public class SprinklerBlockEntity extends BlockEntity {
    public SprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(MainFarming.SPRINKLER_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            FarmingService.queueSprinkler(serverLevel, worldPosition);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel && state.getBlock() instanceof SprinklerBlock sprinkler) {
            FarmClock.get(serverLevel.getServer()).removeSprinkler(serverLevel.dimension(), pos);
            FarmingService.refreshPlantersAround(serverLevel, pos, sprinkler.radius());
        }
    }
}
