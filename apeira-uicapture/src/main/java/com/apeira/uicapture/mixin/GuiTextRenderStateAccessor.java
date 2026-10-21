package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;

/** A queued text draw's private fields, read when it is recorded. */
@Mixin(GuiTextRenderState.class)
public interface GuiTextRenderStateAccessor {
    @Accessor("text")
    FormattedCharSequence apeira$text();

    @Accessor("x")
    int apeira$x();

    @Accessor("y")
    int apeira$y();

    @Accessor("color")
    int apeira$color();

    @Accessor("dropShadow")
    boolean apeira$dropShadow();
}
