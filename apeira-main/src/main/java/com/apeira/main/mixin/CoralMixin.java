package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.world.CoralStabilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Skips a living coral's death check (its scheduled tick, which turns it dead when no water is near) while
 * {@link CoralStabilization#keepsAlive} says it is kept alive. Xenon left coral blocks out; they are kept too.
 */
@Mixin({CoralBlock.class, CoralPlantBlock.class, CoralFanBlock.class, CoralWallFanBlock.class})
public abstract class CoralMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void apeira$keepCoralAlive(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (CoralStabilization.keepsAlive(level, pos)) {
            ci.cancel();
        }
    }
}
