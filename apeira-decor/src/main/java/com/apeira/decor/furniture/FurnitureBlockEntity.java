package com.apeira.decor.furniture;

import com.apeira.decor.paint.PaintBlockEntity;
import com.apeira.decor.registry.DecorBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of a large {@link FurnitureBlock}, which {@code FurnitureRenderer} draws because the piece reaches
 * further than a block model may. It keeps the piece's paint like any {@link PaintBlockEntity}.
 */
public class FurnitureBlockEntity extends PaintBlockEntity {
    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(DecorBlockEntities.FURNITURE.get(), pos, state);
    }
}
