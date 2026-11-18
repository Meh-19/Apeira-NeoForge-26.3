package com.apeira.decor.furniture;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.block.SeatBlock;
import com.apeira.decor.multiblock.Footprint;
import com.apeira.decor.multiblock.Multiblock;
import com.apeira.decor.multiblock.MultiblockController;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * <p>Like an artifact, a piece is the controller of a multiblock piece ({@link MultiblockController}): its
 * {@link FurnitureFootprints footprint} may cover the blocks around it, filled with parts that share its hitboxes, and
 * looking at any of them outlines the whole piece.
 *
 * <p>While {@link FurnitureTunings} are on, a tuned piece takes its shape, placement and seat from its
 * {@link FurnitureTuning} instead.
 */
public class FurnitureBlock extends ApeiraFurnitureBlock implements MultiblockController {
    private final Furniture furniture;
    private @Nullable Footprint footprint;

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

    /** Shapes come from the footprint, cut for a turn only once a piece is placed at it, and tunings can change them. */
    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    /** The piece's footprint, looked up the first time it is needed. */
    @Override
    public Footprint footprint(BlockState state) {
        Footprint known = footprint;
        if (known == null) {
            known = FurnitureFootprints.of(furniture);
            footprint = known;
        }
        return known;
    }

    @Override
    public int turn(BlockState state) {
        return state.getValue(ROTATION);
    }

    @Override
    public boolean partsCollide(BlockState state) {
        return furniture.collision();
    }

    /** A tuned shape while tunings are on, else the part of the footprint in the piece's own block. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape tuned = FurnitureTunings.shape(this, state.getValue(ROTATION));
        return tuned != null ? tuned : controllerShape(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Multiblock.placeParts(level, pos, state, oldState);
    }

    /** The piece's parts go with it, however it is removed. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Multiblock.removeParts(level, pos, state);
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
