package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;

/** A text display's synced text, line width, background colour, opacity and flags. */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("getText")
    Component apeira$getText();

    @Invoker("getLineWidth")
    int apeira$getLineWidth();

    @Invoker("getBackgroundColor")
    int apeira$getBackgroundColor();

    @Invoker("getTextOpacity")
    byte apeira$getTextOpacity();

    @Invoker("getFlags")
    byte apeira$getFlags();
}
