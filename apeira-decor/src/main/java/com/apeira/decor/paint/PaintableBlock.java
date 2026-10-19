package com.apeira.decor.paint;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block the paintbrush and the painting table can paint. Its paint is kept in a {@link PaintBlockEntity}, so the
 * block must also be one of the valid blocks of {@link com.apeira.decor.registry.DecorBlockEntities#PAINT}, or have a
 * block entity type of its own, as furniture and artifacts do.
 *
 * <p>Paint is drawn as CosmoLib drew it, through the item pipeline: a block entity renderer draws the block's item with
 * the block's paint (see {@code FurnitureRenderer}), and the item definition's {@code apeira:paint} tint source tints
 * its {@code tintindex} faces, or draws a finish's effect. Unpainted faces keep their texture's colours.
 */
public interface PaintableBlock extends EntityBlock {
    /**
     * Whether this block takes paint. A block family that shares one class, such as the furniture, implements this
     * interface for all its blocks and answers here for each one.
     */
    default boolean takesPaint() {
        return true;
    }

    @Override
    default @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaintBlockEntity(pos, state);
    }
}
