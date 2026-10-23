package com.apeira.uicapture.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;

/** The player list's header and footer, for the HUD log (F10). */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    @Accessor("header")
    @Nullable Component apeira$header();

    @Accessor("footer")
    @Nullable Component apeira$footer();
}
