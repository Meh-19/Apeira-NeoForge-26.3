package com.apeira.archae.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.registry.ArchaeBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An artifact drawn as an animated GeckoLib model ({@code geckolib/models/archaeology/<name>.geo.json}) instead of its
 * item model, looping one animation of {@code geckolib/animations/archaeology/<name>.animation.json}: Argon's flags,
 * gears, echo bell and relics. Its item still shows the block's static model.
 */
public class AnimatedArtifactBlock extends ArtifactBlock {
    private final String animation;

    /**
     * @param animation the animation it loops, as Argon's client registered it for the block
     * @param wall      the (still) variant placed against the side of a block, or {@code null}
     */
    public AnimatedArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, String animation,
            @Nullable Supplier<? extends Block> wall) {
        super(properties, paintable, Form.FLOOR, wall, null, null);
        this.animation = animation;
    }

    /** The name of the animation the block loops. */
    public String animation() {
        return animation;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnimatedArtifactBlockEntity(ArchaeBlockEntities.ANIMATED_ARTIFACT.get(), pos, state);
    }
}
