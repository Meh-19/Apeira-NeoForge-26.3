package com.apeira.main.realm.balloon;

import com.apeira.decor.paint.PaintBlockEntity;
import com.apeira.main.registry.MainRealmBlocks;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The starter balloon's block entity: it loops the model's {@code RealmBalloonFloating} animation (the balloon and its
 * basket bob over the base) and keeps the balloon's paint, which the realm settings choose ({@link StarterBalloon#paint})
 * and the renderer puts on the model's {@code _tintable} bones. Paint changes reach clients with the block entity update
 * packet, as for any {@link PaintBlockEntity}.
 */
public class StarterBalloonBlockEntity extends PaintBlockEntity implements GeoBlockEntity {
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("RealmBalloonFloating");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public StarterBalloonBlockEntity(BlockPos pos, BlockState state) {
        super(MainRealmBlocks.STARTER_BALLOON_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<StarterBalloonBlockEntity>("controller", 0, test -> test.setAndContinue(FLOAT)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
