package com.apeira.decor.multiblock;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Placing, finding and removing the parts of {@link MultiblockController} pieces. */
public final class Multiblock {
    private Multiblock() {}

    /** A piece: its controller's position and state. */
    public record Piece(BlockPos pos, BlockState state, MultiblockController controller) {
        /** The piece's parts: offsets from the controller and the shape in each block. */
        public Map<BlockPos, VoxelShape> parts() {
            return controller.footprint(state).parts(controller.turn(state));
        }
    }

    /** The piece whose controller is the block at {@code pos}, or {@code null}. */
    public static @Nullable Piece controllerAt(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MultiblockController controller ? new Piece(pos.immutable(), state, controller) : null;
    }

    /**
     * The piece the block at {@code pos} belongs to: the piece itself for a controller, its controller's piece for a
     * part whose controller still stands and still covers it, else {@code null}.
     */
    public static @Nullable Piece pieceAt(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MultiblockPartBlock) {
            return pieceOfPart(level, pos, state);
        }
        return controllerAt(level, pos);
    }

    /** The piece of the part {@code partState} at {@code pos} (which may already be gone), or {@code null}. */
    static @Nullable Piece pieceOfPart(BlockGetter level, BlockPos pos, BlockState partState) {
        BlockPos controllerPos = MultiblockPartBlock.controllerPos(pos, partState);
        Piece piece = controllerAt(level, controllerPos);
        if (piece == null || !piece.parts().containsKey(pos.subtract(controllerPos))) {
            return null;
        }
        return piece;
    }

    /** Whether every block the piece at {@code pos} would cover, besides its own, is free (air, plants, water). */
    public static boolean canPlace(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof MultiblockController controller)) {
            return true;
        }
        for (BlockPos offset : controller.footprint(state).parts(controller.turn(state)).keySet()) {
            if (offset.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos at = pos.offset(offset);
            if (level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at) || !level.getBlockState(at).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Puts the parts of the piece placed at {@code pos} into the free blocks of its footprint, after removing those of
     * the piece it replaced when that was the same block turned another way. Server side; call from {@code onPlace}.
     */
    public static void placeParts(Level level, BlockPos pos, BlockState state, BlockState oldState) {
        if (level.isClientSide() || !(state.getBlock() instanceof MultiblockController controller)) {
            return;
        }
        if (oldState.is(state.getBlock())) {
            if (controller.turn(oldState) == controller.turn(state)) {
                return;
            }
            removeParts(level, pos, oldState);
        }
        Block part = MultiblockPartBlock.block();
        for (BlockPos offset : controller.footprint(state).parts(controller.turn(state)).keySet()) {
            if (offset.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos at = pos.offset(offset);
            BlockState there = level.getBlockState(at);
            if (!there.canBeReplaced() || level.isOutsideBuildHeight(at)) {
                continue;
            }
            boolean water = there.getFluidState().is(Fluids.WATER) && there.getFluidState().isSource();
            level.setBlock(at, MultiblockPartBlock.pointingAt(part.defaultBlockState(), offset.multiply(-1)).setValue(MultiblockPartBlock.WATERLOGGED, water),
                    Block.UPDATE_ALL);
        }
    }

    /** Removes the parts of the piece that stood at {@code pos}; call from {@code affectNeighborsAfterRemoval}. */
    public static void removeParts(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof MultiblockController controller)) {
            return;
        }
        for (BlockPos offset : controller.footprint(state).parts(controller.turn(state)).keySet()) {
            if (offset.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos at = pos.offset(offset);
            BlockState there = level.getBlockState(at);
            if (there.getBlock() instanceof MultiblockPartBlock && MultiblockPartBlock.controllerPos(at, there).equals(pos)) {
                BlockState empty = there.getValue(MultiblockPartBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(at, empty, Block.UPDATE_ALL);
            }
        }
    }
}
