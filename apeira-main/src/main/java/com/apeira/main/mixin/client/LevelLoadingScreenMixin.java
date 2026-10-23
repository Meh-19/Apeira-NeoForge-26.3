package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.client.BlackTransition;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;

/**
 * A loading screen opened while a respawn packet is handled (a dimension change) draws only black: no panorama, portal
 * or end sky, no "Loading terrain..." text, chunk map or progress bar. See {@link BlackTransition}.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    @Unique
    private static final int APEIRA$BLACK = 0xFF000000;

    @Unique
    private boolean apeira$black;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apeira$markTransition(LevelLoadTracker loadTracker, LevelLoadingScreen.Reason reason, CallbackInfo ci) {
        apeira$black = BlackTransition.respawning();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void apeira$blackScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (apeira$black) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), APEIRA$BLACK);
            ci.cancel();
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void apeira$blackBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (apeira$black) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), APEIRA$BLACK);
            ci.cancel();
        }
    }
}
