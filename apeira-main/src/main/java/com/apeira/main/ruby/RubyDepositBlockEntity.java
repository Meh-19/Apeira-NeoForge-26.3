package com.apeira.main.ruby;

import java.util.function.Consumer;

import com.apeira.main.registry.MainRubies;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ruby deposit's block entity: it loops the model's {@code ruby_deposit} animation (the crystal turning half a turn a
 * second and bobbing 3 pixels) and, on the client, keeps the ambient loop playing. It stores nothing.
 */
public class RubyDepositBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("ruby_deposit");
    /** Set by the client ({@code client/ApeiraClient}): starts the ambient loop for a deposit that is not playing it. */
    public static Consumer<RubyDepositBlockEntity> ambience = deposit -> {};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RubyDepositBlockEntity(BlockPos pos, BlockState state) {
        super(MainRubies.RUBY_DEPOSIT_ENTITY.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RubyDepositBlockEntity deposit) {
        ambience.accept(deposit);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RubyDepositBlockEntity>("controller", 0, test -> test.setAndContinue(SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
