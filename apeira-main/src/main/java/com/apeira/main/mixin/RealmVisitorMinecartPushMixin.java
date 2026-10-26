package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.realm.RealmVisitors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

/** Minecarts' own {@code push(Entity)} skips realm visitors too, as {@link RealmVisitorPushMixin} does for the rest. */
@Mixin(AbstractMinecart.class)
public abstract class RealmVisitorMinecartPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apeira$skipVisitorPush(Entity other, CallbackInfo ci) {
        if (RealmVisitors.blocksPush((Entity) (Object) this, other)) {
            ci.cancel();
        }
    }
}
