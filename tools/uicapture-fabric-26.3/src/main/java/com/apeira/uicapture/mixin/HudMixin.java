package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.uicapture.UiCaptureClient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

/**
 * Wraps one HUD frame for a HUD capture (F8 or F10 with no screen open): 26.3's {@code Hud#extractRenderState} is the
 * HUD's whole frame, where NeoForge's build used {@code RenderGuiEvent.Pre} and {@code .Post}.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void apeira$beginHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        UiCaptureClient.beginHud();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void apeira$endHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        UiCaptureClient.endHud();
    }
}
