package com.apeira.archae.block;

import com.apeira.decor.block.SeatBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Argon's {@code SittableBlock}: an artifact a player sits on with a right-click, through apeira-decor's seats. As in
 * Argon, the sitter keeps looking the way they looked, rather than out of the artifact's front.
 */
public class SeatArtifactBlock extends ArtifactBlock implements SeatBlock {
    /**
     * The highest seat, in pixels: a seat entity must stay inside its block, so a seat on the block's top (Argon's
     * rugged stand, 16) sits a tenth of a pixel lower.
     */
    private static final double MAX_SEAT = 15.9;

    private final double seatHeight;

    /** @param seatPixels the height of the seat, in pixels (Argon's {@code seatLevel}) */
    public SeatArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, int seatPixels) {
        super(properties, paintable, Form.FURNITURE);
        this.seatHeight = Math.min(seatPixels, MAX_SEAT) / 16.0;
    }

    @Override
    public double seatHeight() {
        return seatHeight;
    }

    @Override
    public float seatYaw(BlockState state, Entity sitter) {
        return sitter.getYRot();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return SeatBlock.use(state, level, pos, player);
    }
}
