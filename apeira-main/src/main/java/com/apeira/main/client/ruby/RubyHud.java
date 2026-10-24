package com.apeira.main.client.ruby;

import com.apeira.main.ruby.Rubies;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The HUD's ruby counter, where Origin Realms draws it in the action bar: the balance and the ruby icon over the right
 * end of the food row, the icon's left edge 79 pixels right of the screen's centre (the hotbar's right end is at 91) and
 * the digits ending before it, 49 pixels above the bottom (the icon one higher), in the font textures' own colours (drawn
 * white, so nothing tints them) with the text shadow. Shown in survival, adventure and creative (where the food bar it
 * sits over is not drawn), not in spectator, while the HUD is not hidden (F1).
 */
public final class RubyHud {
    private static final int ICON_X = 79;
    private static final int BOTTOM = 49;
    /** White leaves the glyphs' own colours as they are. */
    private static final int COLOR = 0xFFFFFFFF;

    private RubyHud() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.isSpectator() || minecraft.gui.hud.isHidden()) {
            return;
        }
        Font font = minecraft.font;
        Component digits = RubyCounter.hudDigits(Rubies.get(minecraft.player));
        int iconX = graphics.guiWidth() / 2 + ICON_X;
        int y = graphics.guiHeight() - BOTTOM;
        Component counter = Component.empty().append(digits).append(RubyCounter.hudIcon());
        // The gap glyph after the digits is 1 wide, so the icon starts one pixel after the digits end.
        graphics.text(font, counter, iconX - font.width(digits) - 1, y, COLOR, true);
    }
}
