package com.apeira.decor.client;

import com.apeira.decor.paint.Paint;
import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Tints a placed paintable block's {@code tintindex} 0 faces with the paint in its {@link PaintBlockEntity}. */
public final class PaintBlockTint implements BlockTintSource {
    public static final PaintBlockTint INSTANCE = new PaintBlockTint();

    private PaintBlockTint() {}

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PaintBlockEntity block && block.paint() != null) {
            return Paint.tint(block.paint());
        }
        return -1;
    }
}
