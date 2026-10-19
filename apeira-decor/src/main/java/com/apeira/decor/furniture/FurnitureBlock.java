package com.apeira.decor.furniture;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.block.SeatBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A furniture piece of {@link FurnitureCatalogue}, on CosmoLib's furniture system ({@link ApeiraFurnitureBlock}): turned
 * in 45 degree steps to face the player who placed it, with its catalogue shape turned to match. Every piece has a
 * {@link FurnitureBlockEntity}, which keeps its paint and which {@code FurnitureRenderer} draws. Seats are
 * {@link Seat}s.
 *
 * <p>While {@link FurnitureTunings} are on, a tuned piece takes its shape, placement and seat from its
 * {@link FurnitureTuning} instead.
 */
public class FurnitureBlock extends ApeiraFurnitureBlock {
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
        return new FurnitureBlockEntity(pos, state);
    }

    /** Tunable shapes can change at any time, so they are not cached while tunings are on. */
    @Override
    public boolean hasDynamicShape() {
        return super.hasDynamicShape() || FurnitureTunings.enabled();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape tuned = FurnitureTunings.shape(this, state.getValue(ROTATION));
        return tuned != null ? tuned : super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FurnitureTuning tuning = FurnitureTunings.get(this);
        if (tuning == null) {
            return super.getStateForPlacement(context);
        }
        if (!FurniturePlacement.allows(tuning.placements(), context.getClickedFace())) {
            return null;
        }
        int rotation = FurniturePlacement.DEFAULT.in(tuning.placements()) ? tuning.rotation() : rotation(context) + tuning.rotation();
        return defaultBlockState().setValue(ROTATION, Math.floorMod(rotation, ROTATIONS));
    }

    /** Furniture with a seat: right-clicking it sits the player down. */
    public static class Seat extends FurnitureBlock implements SeatBlock {
        public Seat(BlockBehaviour.Properties properties, Furniture furniture) {
            super(properties, furniture);
        }

        @Override
        public double seatHeight() {
            FurnitureTuning tuning = FurnitureTunings.get(this);
            return Math.max(0.0, tuning != null ? tuning.seatHeight() : furniture().seatHeight()) / 16.0;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
            return SeatBlock.use(state, level, pos, player);
        }
    }
}
