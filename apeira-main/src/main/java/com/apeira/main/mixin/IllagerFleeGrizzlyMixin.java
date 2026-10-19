package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.entity.GrizzlyBearEntity;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;

/**
 * Pillagers, vindicators and evokers flee from a roaring {@link GrizzlyBearEntity} (within 12 blocks, at 1.0 walking and 1.5 sprinting speed), as
 * in Xenon. They ignore bears that are not roaring.
 */
@Mixin({Pillager.class, Vindicator.class, Evoker.class})
public abstract class IllagerFleeGrizzlyMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void apeira$addBearFleeGoal(CallbackInfo ci) {
        Monster self = (Monster) (Object) this;
        self.goalSelector.addGoal(3, new AvoidEntityGoal<>(self, GrizzlyBearEntity.class,
                bear -> bear instanceof GrizzlyBearEntity b && b.isRoaring(),
                12.0F, 1.0, 1.5,
                bear -> bear instanceof GrizzlyBearEntity b && b.isRoaring()));
    }
}
