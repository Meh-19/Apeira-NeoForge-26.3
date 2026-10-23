package com.apeira.uicapture.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;

/** The action bar message and the title the HUD keeps private, for the HUD log (F10). */
@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("overlayMessageString")
    @Nullable Component apeira$overlayMessage();

    @Accessor("overlayMessageTime")
    int apeira$overlayMessageTime();

    @Accessor("title")
    @Nullable Component apeira$title();

    @Accessor("subtitle")
    @Nullable Component apeira$subtitle();

    @Accessor("titleTime")
    int apeira$titleTime();
}
