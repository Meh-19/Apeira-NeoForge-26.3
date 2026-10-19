package com.apeira.archae.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of an {@link AnimatedArtifactBlock} ({@code apeira:generic_animated_furniture}, Argon's id): an
 * artifact block entity that loops its block's animation.
 */
public class AnimatedArtifactBlockEntity extends ArtifactBlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedArtifactBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String name = getBlockState().getBlock() instanceof AnimatedArtifactBlock block ? block.animation() : "idle";
        RawAnimation animation = RawAnimation.begin().thenLoop(name);
        controllers.add(new AnimationController<AnimatedArtifactBlockEntity>("controller", 0, test -> test.setAndContinue(animation)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
