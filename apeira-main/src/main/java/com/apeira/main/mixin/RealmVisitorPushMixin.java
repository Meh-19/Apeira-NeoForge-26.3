package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.realm.RealmVisitors;

import net.minecraft.world.entity.Entity;

/**
 * Realm visitors ({@link RealmVisitors}) neither push nor are pushed: every collision push between two entities is
 * {@code a.push(b)}, called both ways, so skipping it when either is a visitor covers mobs, players and boats.
 * Minecarts override it, see {@link RealmVisitorMinecartPushMixin}.
 */
@Mixin(Entity.class)
public abstract class RealmVisitorPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apeira$skipVisitorPush(Entity other, CallbackInfo ci) {
        if (RealmVisitors.blocksPush((Entity) (Object) this, other)) {
            ci.cancel();
        }
    }
}
