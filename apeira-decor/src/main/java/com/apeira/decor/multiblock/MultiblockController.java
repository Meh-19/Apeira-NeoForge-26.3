package com.apeira.decor.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block that is the controller of a multiblock piece: the block that holds the piece's state, block entity, drops and
 * seats, with {@link MultiblockPartBlock}s in the other blocks of its {@link Footprint}. Parts send right-clicks,
 * breaking and pick-block to it, and breaking any part breaks the whole piece.
 *
 * <p>A controller block wires itself up by calling {@link Multiblock#placeParts} from {@code onPlace},
 * {@link Multiblock#removeParts} from {@code affectNeighborsAfterRemoval}, answering its own {@code getShape} with
 * {@link #controllerShape}, and checking {@link Multiblock#canPlace} before it is placed (for a block item, in
 * {@code BlockItem#canPlace}).
 */
public interface MultiblockController {
    /** The piece's footprint, at turn 0. */
    Footprint footprint(BlockState state);

    /** How far the placed piece is turned, in 45 degree steps (0-7), as {@link Footprint#parts(int)} takes it. */
    int turn(BlockState state);

    /** Whether the parts stop entities, as the piece does. */
    boolean partsCollide(BlockState state);

    /** The piece's shape inside the controller's own block. */
    default VoxelShape controllerShape(BlockState state) {
        return footprint(state).shape(turn(state), BlockPos.ZERO);
    }
}
