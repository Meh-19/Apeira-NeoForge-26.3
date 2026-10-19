package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.registry.MainEffects;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Draws nothing at all (model, armour, held items, name tag, cape, shadow) for a player in Shadow Mode, where vanilla
 * invisibility would still show the armour. The client only knows its own player's effects, so this hides the player
 * from themselves in third person, as in Xenon; other players see vanilla invisibility.
 */
@Mixin(EntityRenderer.class)
public abstract class ShadowPlayerRenderMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void apeira$hidePlayerInShadowMode(Entity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks,
            CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player && player.hasEffect(MainEffects.SHADOW_MODE)) {
            cir.setReturnValue(false);
        }
    }
}
