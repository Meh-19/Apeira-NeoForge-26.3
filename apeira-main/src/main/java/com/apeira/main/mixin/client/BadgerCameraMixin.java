package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.client.crate.BadgerCrateClient;

import net.minecraft.client.Camera;

/**
 * Locks the camera to the Badger Crate opening's recorded track while it plays
 * ({@link BadgerCrateClient#cameraPose}), after vanilla and NeoForge's angle event have placed it on the player.
 */
@Mixin(Camera.class)
public abstract class BadgerCameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void apeira$lockBadgerCamera(float partialTicks, CallbackInfo ci) {
        double[] pose = BadgerCrateClient.cameraPose(partialTicks);
        if (pose != null) {
            setRotation((float) pose[3], (float) pose[4]);
            setPosition(pose[0], pose[1], pose[2]);
        }
    }
}
