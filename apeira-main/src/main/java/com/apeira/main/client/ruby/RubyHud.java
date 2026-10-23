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
 * the digits ending before it, 49 pixels above the bottom (the icon one higher), tinted {@code #FEFD04} with the text
 * shadow. Shown like the food bar: in survival and adventure, while the HUD is not hidden (F1).
 */
public final class RubyHud {
    private static final int ICON_X = 79;
    private static final int BOTTOM = 49;
    private static final int COLOR = 0xFFFEFD04;

    private RubyHud() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer() || minecraft.gui.hud.isHidden()) {
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
