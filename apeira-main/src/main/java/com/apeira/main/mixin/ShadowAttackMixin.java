package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.Apeira;
import com.apeira.main.effect.ShadowModeManager;
import com.apeira.main.item.ShadowSwordItem;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Xenon's stealth damage: an attack does 1.5 times the damage in Shadow Mode, and 1.5 times with the shadow sword against
 * a mob not targeting the attacker (2.25 for both), through a multiplier on attack damage held only for the attack.
 * The attack then ends Shadow Mode.
 */
@Mixin(Player.class)
public abstract class ShadowAttackMixin {
    @Unique
    private static final Identifier APEIRA$SHADOW_BONUS = Apeira.REGISTRAR.id("shadow_attack_bonus");

    @Inject(method = "attack", at = @At("HEAD"))
    private void apeira$beforeShadowAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        float multiplier = 1.0F;
        if (ShadowModeManager.isInShadowMode(self)) {
            multiplier *= ShadowModeManager.STEALTH_DAMAGE_MULTIPLIER;
        }
        if (self.getMainHandItem().getItem() instanceof ShadowSwordItem && target instanceof Mob mob && mob.getTarget() != self) {
            multiplier *= ShadowModeManager.STEALTH_DAMAGE_MULTIPLIER;
        }
        if (multiplier > 1.0F) {
            AttributeInstance damage = self.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.removeModifier(APEIRA$SHADOW_BONUS);
                damage.addTransientModifier(new AttributeModifier(APEIRA$SHADOW_BONUS, multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void apeira$afterShadowAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        AttributeInstance damage = self.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(APEIRA$SHADOW_BONUS);
        }
        if (self instanceof ServerPlayer player && ShadowModeManager.isInShadowMode(player)) {
            ShadowModeManager.deactivate(player);
        }
    }
}
