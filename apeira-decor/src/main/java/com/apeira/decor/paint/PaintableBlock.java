package com.apeira.decor.paint;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block the paintbrush and the painting table can paint. Its paint is kept in a {@link PaintBlockEntity}, so the
 * block must also be one of the valid blocks of {@link com.apeira.decor.registry.DecorBlockEntities#PAINT}.
 *
 * <p>Paint tints the faces of the block's model with {@code "tintindex": 0}; the client registers the tint for each
 * paintable block in {@link com.apeira.decor.client.ApeiraDecorClient}, and the block item's model needs the
 * {@code apeira:paint} tint source. Unpainted faces keep their texture's colours.
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
