package com.apeira.decor.furniture;

import com.apeira.decor.registry.DecorBlockEntities;
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
 * The block entity of an {@link AnimatedFurnitureBlock} ({@code apeira:generic_animated_furniture}, Argon's id): an
 * piece block entity that loops its block's animation.
 */
public class AnimatedFurnitureBlockEntity extends FurnitureBlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedFurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(DecorBlockEntities.ANIMATED_FURNITURE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String name = getBlockState().getBlock() instanceof AnimatedFurnitureBlock block ? block.animation() : "idle";
        RawAnimation animation = RawAnimation.begin().thenLoop(name);
        controllers.add(new AnimationController<AnimatedFurnitureBlockEntity>("controller", 0, test -> test.setAndContinue(animation)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
