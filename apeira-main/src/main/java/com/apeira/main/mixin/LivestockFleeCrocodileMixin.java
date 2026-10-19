package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.entity.CrocodileEntity;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;

/**
 * Makes chickens, cows (and mooshrooms), sheep, pigs and rabbits flee from crocodiles within 10 blocks, as Xenon did.
 * Cows' goals are registered by {@link AbstractCow} in 26.x; in Xenon's 1.21.1 mooshrooms were cows too.
 */
@Mixin({Chicken.class, AbstractCow.class, Sheep.class, Pig.class, Rabbit.class})
public abstract class LivestockFleeCrocodileMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void apeira$addCrocodileFleeGoal(CallbackInfo ci) {
        Animal self = (Animal) (Object) this;
        self.goalSelector.addGoal(1, new AvoidEntityGoal<>(self, CrocodileEntity.class, 10.0F, 1.25, 1.5));
    }
}
