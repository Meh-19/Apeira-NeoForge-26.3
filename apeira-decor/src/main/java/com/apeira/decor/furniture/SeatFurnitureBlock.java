package com.apeira.decor.furniture;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.SeatBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Argon's {@code SittableBlock}: a furniture piece a player sits on with a right-click, through apeira-decor's seats. As in
 * Argon, the sitter keeps looking the way they looked, rather than out of the piece's front.
 */
public class SeatFurnitureBlock extends FurnitureBlock implements SeatBlock {
    /**
     * The highest seat, in pixels: a seat entity must stay inside its block, so a seat on the block's top (Argon's
     * rugged stand, 16) sits a tenth of a pixel lower.
     */
    private static final double MAX_SEAT = 15.9;

    private final double seatHeight;
    /** Seats at turn 0, in pixels from the block's bottom north-west corner, or {@code null} for one at the centre. */
    private @Nullable List<Vec3> seatPixels;

    /** @param seatPixels the height of the seat, in pixels (Argon's {@code seatLevel}) */
    public SeatFurnitureBlock(BlockBehaviour.Properties properties, boolean paintable, int seatPixels) {
        this(properties, paintable, Form.FURNITURE, seatPixels);
    }

    /** A seat drawn in another {@link Form}'s display mode (the froggy chair lies on the floor). */
    public SeatFurnitureBlock(BlockBehaviour.Properties properties, boolean paintable, Form form, int seatPixels) {
        super(properties, paintable, form);
        this.seatHeight = Math.min(seatPixels, MAX_SEAT) / 16.0;
    }

    @Override
    public double seatHeight() {
        return seatHeight;
    }

    /**
     * Gives the piece several seats (the bathtub's two), each {@code x, y, z} in pixels from the block's bottom
     * north-west corner at turn 0, as the footprint table measures the piece; they turn with the piece as its
     * footprint does ({@code Footprint}). Returns this block.
     */
    public SeatFurnitureBlock seats(double... pixels) {
        List<Vec3> seats = new ArrayList<>();
        for (int i = 0; i + 2 < pixels.length; i += 3) {
            seats.add(new Vec3(pixels[i], pixels[i + 1], pixels[i + 2]));
        }
        seatPixels = List.copyOf(seats);
        return this;
    }

    @Override
    public List<Vec3> seats(BlockState state) {
        if (seatPixels == null) {
            return SeatBlock.super.seats(state);
        }
        double angle = Math.toRadians(turn(state) * 45.0);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        List<Vec3> seats = new ArrayList<>(seatPixels.size());
        for (Vec3 seat : seatPixels) {
            double dx = seat.x / 16.0 - 0.5;
            double dz = seat.z / 16.0 - 0.5;
            seats.add(new Vec3(0.5 + dx * cos - dz * sin, seat.y / 16.0, 0.5 + dx * sin + dz * cos));
        }
        return seats;
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
