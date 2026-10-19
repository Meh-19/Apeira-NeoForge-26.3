package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.effect.ShadowModeManager;
import com.apeira.main.registry.MainEffects;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Xenon's Shadow Mode vignette, drawn over the HUD: a dark purple fog fading in from the screen edges over 15 ticks, from
 * 24 bands of falling opacity, darker corners and a faint tint over the whole screen.
 */
@Mixin(Hud.class)
public abstract class ShadowVignetteMixin {
    @Unique
    private static final int APEIRA$FADE_IN_TICKS = 15;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void apeira$renderShadowVignette(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        MobEffectInstance effect = minecraft.player.getEffect(MainEffects.SHADOW_MODE);
        if (effect == null) {
            return;
        }
        int elapsed = ShadowModeManager.SHADOW_DURATION_TICKS - effect.getDuration();
        float fade = Math.min((float) elapsed / APEIRA$FADE_IN_TICKS, 1.0F);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int layers = 24;
        int maxEdge = Math.min(width, height) / 3;
        int thickness = Math.max(maxEdge / layers, 1);
        for (int i = 0; i < layers; i++) {
            float t = (float) i / layers;
            int inset = (int) (t * maxEdge);
            int alpha = (int) ((1.0F - t) * (1.0F - t) * fade * 160);
            if (alpha <= 0) {
                continue;
            }
            int color = alpha << 24 | 40 << 16 | 70;
            graphics.fill(inset, inset, width - inset, inset + thickness, color);
            graphics.fill(inset, height - inset - thickness, width - inset, height - inset, color);
            graphics.fill(inset, inset, inset + thickness, height - inset, color);
            graphics.fill(width - inset - thickness, inset, width - inset, height - inset, color);
        }

        int cornerColor = (int) (fade * 120) << 24 | 20 << 16 | 40;
        apeira$fillCorner(graphics, 0, 0, maxEdge, maxEdge, cornerColor);
        apeira$fillCorner(graphics, width - maxEdge, 0, width, maxEdge, cornerColor);
        apeira$fillCorner(graphics, 0, height - maxEdge, maxEdge, height, cornerColor);
        apeira$fillCorner(graphics, width - maxEdge, height - maxEdge, width, height, cornerColor);

        graphics.fill(0, 0, width, height, (int) (fade * 25) << 24 | 30 << 16 | 50);
    }

    /** Darkens a corner square with five shrinking fills, each fainter than the last. */
    @Unique
    private static void apeira$fillCorner(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int layers = 5;
        int size = Math.min(x2 - x1, y2 - y1);
        for (int i = 0; i < layers; i++) {
            int shrink = (int) ((float) i / layers * size / 2);
            int alpha = (color >>> 24 & 0xFF) / (i + 1);
            graphics.fill(x1 + shrink, y1 + shrink, x2 - shrink, y2 - shrink, alpha << 24 | color & 0xFFFFFF);
        }
    }
}
