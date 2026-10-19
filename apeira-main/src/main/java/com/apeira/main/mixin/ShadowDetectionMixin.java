package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.effect.ShadowModeManager;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Mobs can't target a player in Shadow Mode, and each shadow armour piece a player wears cuts 3 blocks off the follow
 * range a mob can target them from (never below 2). Only mobs targeting players are affected.
 */
@Mixin(LivingEntity.class)
public abstract class ShadowDetectionMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void apeira$reduceShadowDetection(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Mob mob) || !(target instanceof Player player)) {
            return;
        }
        if (ShadowModeManager.isInShadowMode(player)) {
            cir.setReturnValue(false);
            return;
        }
        float reduction = ShadowModeManager.getDetectionReduction(player);
        if (reduction <= 0) {
            return;
        }
        double range = Math.max(mob.getAttributeValue(Attributes.FOLLOW_RANGE) - reduction, 2.0);
        if (mob.distanceToSqr(target) > range * range) {
            cir.setReturnValue(false);
        }
    }
}
