package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.uicapture.UiCapture;

import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;

/**
 * Records every text draw during a capture. All text reaches the render state here, including widget labels that
 * {@code GuiGraphics}' text collectors add without going through its {@code drawString} methods.
 */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
    @Inject(method = "submitText", at = @At("HEAD"))
    private void apeira$recordText(GuiTextRenderState textState, CallbackInfo ci) {
        if (UiCapture.recording()) {
            UiCapture.text(textState.pose, textState.text, textState.x, textState.y, textState.color, textState.dropShadow);
        }
    }
}
