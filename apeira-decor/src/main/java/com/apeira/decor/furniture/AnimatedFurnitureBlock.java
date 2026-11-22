package com.apeira.decor.furniture;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A furniture piece drawn as an animated GeckoLib model ({@code geckolib/models/<folder>/<name>.geo.json}, texture
 * {@code textures/block/<folder>/<name>.png}) instead of its item model, looping one animation of
 * {@code geckolib/animations/<folder>/<name>.animation.json}: Argon's flags, gears, echo bell and relics, for example.
 * Its item still shows the block's static model.
 */
public class AnimatedFurnitureBlock extends FurnitureBlock {
    private final String folder;
    private final String animation;

    /**
     * @param folder    the folder its GeckoLib files and texture are in, such as {@code archaeology}
     * @param animation the animation it loops
     * @param wall      the (still) variant placed against the side of a block, or {@code null}
     */
    public AnimatedFurnitureBlock(BlockBehaviour.Properties properties, String folder, boolean paintable, String animation,
            @Nullable Supplier<? extends Block> wall) {
        this(properties, folder, paintable, animation, wall, null, null);
    }

    /**
     * @param wall    the variant placed against the side of a block (still or animated), or {@code null}
     * @param ceiling the variant placed under a block, or {@code null} (the circus flag's still ceiling flag)
     * @param drop    the piece a variant drops, or {@code null} for itself
     */
    public AnimatedFurnitureBlock(BlockBehaviour.Properties properties, String folder, boolean paintable, String animation,
            @Nullable Supplier<? extends Block> wall, @Nullable Supplier<? extends Block> ceiling, @Nullable Supplier<? extends Block> drop) {
        super(properties, paintable, Form.FLOOR, wall, ceiling, drop);
        this.folder = folder;
        this.animation = animation;
    }

    public String folder() {
        return folder;
    }

    /** The name of the animation the block loops. */
    public String animation() {
        return animation;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnimatedFurnitureBlockEntity(pos, state);
    }
}
