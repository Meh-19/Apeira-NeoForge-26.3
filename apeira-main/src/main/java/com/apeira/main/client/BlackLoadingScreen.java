package com.apeira.main.client;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Dimension changes show a plain black screen, as on Origin Realms, instead of vanilla's "Loading terrain..." screen
 * with its panorama or portal background: the loading screen still opens, waits for the level and closes as usual, but
 * only black is drawn over it.
 */
public final class BlackLoadingScreen {
    private static final int BLACK = 0xFF000000;

    private BlackLoadingScreen() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BlackLoadingScreen::onRender);
    }

    private static void onRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof LevelLoadingScreen screen) {
            event.getGuiGraphics().fill(0, 0, screen.width, screen.height, BLACK);
            event.setCanceled(true);
        }
    }
}
