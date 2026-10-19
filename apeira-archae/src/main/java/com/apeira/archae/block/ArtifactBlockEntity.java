package com.apeira.archae.block;

import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of an {@link ArtifactBlock} ({@code apeira:artifact_display}, Argon's {@code artifact_display}),
 * which {@code ArtifactRenderer} draws. It keeps the artifact's paint like any {@link PaintBlockEntity}.
 */
public class ArtifactBlockEntity extends PaintBlockEntity {
    public ArtifactBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
