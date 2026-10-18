package com.apeira.main.block.entity;

import com.apeira.main.block.SpikeTrapBlock;
import com.apeira.main.registry.MainBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Hurts what stands in a spike trap's spikes; the trap's state is all it needs, so it saves nothing. */
public class SpikeTrapBlockEntity extends BlockEntity {
    private static final float DAMAGE_PER_TICK = 1.0F;

    public SpikeTrapBlockEntity(BlockPos pos, BlockState state) {
        super(MainBlockEntities.SPIKE_TRAP.get(), pos, state);
    }

    /** While the spikes are out, hurts every living thing in the block in front of the trap, or touching it. */
    public static void serverTick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !state.getValue(SpikeTrapBlock.SPIKES_OUT)) {
            return;
        }
        AABB spikes = new AABB(pos.relative(state.getValue(SpikeTrapBlock.FACING))).inflate(0.1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, spikes, LivingEntity::isAlive)) {
            entity.hurtServer(serverLevel, level.damageSources().cactus(), DAMAGE_PER_TICK);
        }
    }
}
