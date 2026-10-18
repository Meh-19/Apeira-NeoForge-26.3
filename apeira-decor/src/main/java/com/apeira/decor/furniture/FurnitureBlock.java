package com.apeira.decor.furniture;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.block.SeatBlock;
import com.apeira.decor.paint.PaintBlockEntity;
import com.apeira.decor.paint.PaintableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A furniture piece of {@link FurnitureCatalogue}: faces the player who placed it, with its catalogue shape turned to
 * match. Paintable pieces keep their paint in a {@link PaintBlockEntity}; large pieces in a
 * {@link FurnitureBlockEntity}, which {@code FurnitureRenderer} draws. Seats are {@link Seat}s.
 */
public class FurnitureBlock extends ApeiraFurnitureBlock implements PaintableBlock {
    private final Furniture furniture;

    public FurnitureBlock(BlockBehaviour.Properties properties, Furniture furniture) {
        super(properties, furniture.shape());
        this.furniture = furniture;
    }

    /** A furniture block for {@code furniture}: a {@link Seat} if it has one. */
    public static FurnitureBlock create(BlockBehaviour.Properties properties, Furniture furniture) {
        return furniture.seat() ? new Seat(properties, furniture) : new FurnitureBlock(properties, furniture);
    }

    public Furniture furniture() {
        return furniture;
    }

    @Override
    public boolean takesPaint() {
        return furniture.paintable();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (furniture.large() != null) {
            return new FurnitureBlockEntity(pos, state);
        }
        return furniture.paintable() ? new PaintBlockEntity(pos, state) : null;
    }

    /** Furniture with a seat: right-clicking it sits the player down. */
    public static class Seat extends FurnitureBlock implements SeatBlock {
        public Seat(BlockBehaviour.Properties properties, Furniture furniture) {
            super(properties, furniture);
        }

        @Override
        public double seatHeight() {
            return furniture().seatHeight() / 16.0;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            return SeatBlock.use(state, level, pos, player);
        }
    }
}
