package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.effect.ShadowModeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shadow tools break blocks silently: {@link Block#playerWillDestroy} is skipped for them, so no break sound or particles
 * (level event 2001), no {@code block_destroy} game event for sculk sensors and shriekers, and no angered piglins. The
 * block is still removed and drops as usual. Blocks that override it (doors, beds, double plants) still run their own
 * part, such as breaking the other half.
 */
@Mixin(Block.class)
public abstract class SilentBlockBreakMixin {
    @Inject(method = "playerWillDestroy", at = @At("HEAD"), cancellable = true)
    private void apeira$silenceShadowToolBreak(Level level, BlockPos pos, BlockState state, Player player,
            CallbackInfoReturnable<BlockState> cir) {
        if (ShadowModeManager.isShadowTool(player.getMainHandItem())) {
            cir.setReturnValue(state);
        }
    }
}
