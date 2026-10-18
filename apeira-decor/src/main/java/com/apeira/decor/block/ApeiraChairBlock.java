package com.apeira.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Furniture a player sits on by right-clicking it. Sitting mounts the player on an
 * {@link com.apeira.decor.entity.ApeiraSeatEntity} at the centre of the chair, at the height of its seat; the seat
 * removes itself when the player gets up or the chair is broken. A chair seats one player at a time, and sneaking players don't sit down, so they can place blocks against it.
 */
public abstract class ApeiraChairBlock extends ApeiraFurnitureBlock implements SeatBlock {
    private final double seatHeight;

    /**
     * @param northShape the chair's shape, facing north
     * @param seatHeight the height of the top of the seat above the bottom of the block, in pixels (0 to 16)
     */
    protected ApeiraChairBlock(BlockBehaviour.Properties properties, VoxelShape northShape, double seatHeight) {
        super(properties, northShape);
        if (seatHeight < 0 || seatHeight >= 16) {
            throw new IllegalArgumentException("Seat height must be within the block (0 to 16 pixels), was " + seatHeight);
        }
        this.seatHeight = seatHeight / 16.0;
    }

    @Override
    public double seatHeight() {
        return seatHeight;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return SeatBlock.use(state, level, pos, player);
    }
}
