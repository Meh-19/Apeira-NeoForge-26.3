package com.apeira.main.item;

import com.apeira.main.world.CoralStabilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Xenon's dolomite dust, used like honeycomb: on a living coral it keeps that coral alive out of water ({@link CoralStabilization}). Each use costs one dust.
 */
public class DolomiteDustItem extends Item {
    public DolomiteDustItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        if (CoralStabilization.isLivingCoral(level.getBlockState(pos))) {
            CoralStabilization stabilization = CoralStabilization.get(level);
            if (stabilization.isStabilized(pos)) {
                return InteractionResult.PASS;
            }
            stabilization.add(pos);
        } else {
            return InteractionResult.PASS;
        }
        level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.01);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.SUCCESS_SERVER;
    }
}
