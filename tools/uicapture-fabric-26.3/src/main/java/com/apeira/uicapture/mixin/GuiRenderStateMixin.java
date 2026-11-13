package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.uicapture.UiCapture;

import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;

/**
 * Records every text draw during a capture. All text reaches the render state here, including widget labels that
 * {@code GuiGraphicsExtractor}'s text collectors add without going through its {@code text} methods.
 */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
    @Inject(method = "addText", at = @At("HEAD"))
    private void apeira$recordText(GuiTextRenderState textState, CallbackInfo ci) {
        if (UiCapture.recording()) {
            GuiTextRenderStateAccessor text = (GuiTextRenderStateAccessor) (Object) textState;
            UiCapture.text(textState.pose, text.apeira$text(), text.apeira$x(), text.apeira$y(), text.apeira$color(), text.apeira$dropShadow());
        }
    }
}
