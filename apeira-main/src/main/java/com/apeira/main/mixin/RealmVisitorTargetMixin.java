package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.realm.RealmVisitors;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Mobs can't attack a realm visitor ({@link RealmVisitors}). Target goals, brain sensors and the checks that drop a
 * target all ask {@code canAttack}, so mobs neither pick, chase nor keep a visitor as their target.
 */
@Mixin(LivingEntity.class)
public abstract class RealmVisitorTargetMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void apeira$ignoreVisitors(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Mob && RealmVisitors.isVisitor(target)) {
            cir.setReturnValue(false);
        }
    }
}
