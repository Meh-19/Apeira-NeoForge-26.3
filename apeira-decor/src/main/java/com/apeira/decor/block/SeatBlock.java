package com.apeira.decor.block;

import com.apeira.decor.entity.ApeiraSeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block a player can sit on through {@link com.apeira.decor.entity.ApeiraSeatEntity}: furniture facing one of the
 * four horizontal directions ({@link net.minecraft.world.level.block.HorizontalDirectionalBlock#FACING}), with a seat
 * at its centre. The sitter looks out of its front.
 */
public interface SeatBlock {
    /** The height of the top of the seat above the bottom of the block, in blocks. */
    double seatHeight();

    /**
     * Right-clicking a seat sits the player down. Sneaking players and players already riding don't sit, so they can
     * place blocks against it.
     */
    static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isSecondaryUseActive() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return ApeiraSeatEntity.sit(level, pos, state, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }
}
