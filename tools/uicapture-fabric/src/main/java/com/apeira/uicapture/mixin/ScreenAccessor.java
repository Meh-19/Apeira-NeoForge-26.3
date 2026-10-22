package com.apeira.uicapture.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;

/** The renderables a screen keeps private, so the capture can list widgets that are drawn but not interactive. */
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("renderables")
    List<Renderable> apeira$renderables();
}
