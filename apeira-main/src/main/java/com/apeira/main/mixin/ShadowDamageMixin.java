package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.effect.ShadowModeManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cancels all damage to a player in Shadow Mode. A player wearing a full shadow set, off cooldown, enters Shadow Mode
 * when hit instead of taking the hit.
 */
@Mixin(LivingEntity.class)
public abstract class ShadowDamageMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void apeira$shadowModeOnDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        if (ShadowModeManager.isInShadowMode(player)) {
            cir.setReturnValue(false);
        } else if (ShadowModeManager.hasFullShadowSet(player) && ShadowModeManager.canActivate(player)) {
            ShadowModeManager.activate(player);
            cir.setReturnValue(false);
        }
    }
}
