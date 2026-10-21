package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.uicapture.UiCaptureClient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/** Wraps one HUD frame for a HUD capture (F8 with no screen open). */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void apeira$beginHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        UiCaptureClient.beginHud();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void apeira$endHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        UiCaptureClient.endHud();
    }
}
