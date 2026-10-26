package com.apeira.main.client.ruby;

import com.apeira.main.ruby.Rubies;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

/**
 * The HUD's ruby counter, where Origin Realms draws it in the action bar: the balance and the ruby icon over the right
 * end of the food row, the icon's left edge 79 pixels right of the screen's centre (the hotbar's right end is at 91) and
 * the digits ending before it, 49 pixels above the bottom (the icon one higher), in the font textures' own colours (drawn
 * white, so nothing tints them) with the text shadow. In creative, where the hearts, food and experience bars are not
 * drawn, it moves down to sit as far above the hotbar as it sits above the food row otherwise (32 pixels above the
 * bottom). Not shown in spectator or while the HUD is hidden (F1). With the ruby ticker on ({@link RubyTickerAnimation}),
 * the counter shows the ticker's balance and its +/- banner floats over it.
 */
public final class RubyHud {
    private static final int ICON_X = 79;
    private static final int BOTTOM = 49;
    /** {@link #BOTTOM} less the food row and experience bar (17 pixels) that creative leaves out above the hotbar. */
    private static final int CREATIVE_BOTTOM = 32;
    /** White leaves the glyphs' own colours as they are. */
    private static final int COLOR = 0xFFFFFFFF;

    private static final int BANNER_RISE = 9;
    /** Half the banner's height: it scales about its right end's middle. */
    private static final int BANNER_MIDDLE = 4;
    private static final float BANNER_ALPHA = 0.85F;

    private RubyHud() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.isSpectator() || minecraft.gui.hud.isHidden()) {
            return;
        }
        Font font = minecraft.font;
        long now = Util.getMillis();
        long balance = Rubies.get(minecraft.player);
        if (RubyTickerAnimation.enabled()) {
            RubyTickerAnimation.update(now);
            balance = RubyTickerAnimation.displayed(balance);
        }
        Component digits = RubyCounter.hudDigits(balance);
        int iconX = graphics.guiWidth() / 2 + ICON_X;
        int y = graphics.guiHeight() - (minecraft.player.isCreative() ? CREATIVE_BOTTOM : BOTTOM);
        Component counter = Component.empty().append(digits).append(RubyCounter.hudIcon());
        // The gap glyph after the digits is 1 wide, so the icon starts one pixel after the digits end.
        graphics.text(font, counter, iconX - font.width(digits) - 1, y, COLOR, true);
        if (RubyTickerAnimation.enabled() && RubyTickerAnimation.active()) {
            banner(graphics, font, iconX + font.width(RubyCounter.hudIcon()), y, now);
        }
    }

    /**
     * The ruby ticker's banner, right-aligned with the counter's icon and {@link #BANNER_RISE} pixels over it,
     * semi-transparent ({@link #BANNER_ALPHA}); its fade, scale and float come from {@link RubyTickerAnimation} and it
     * scales about its right end, so it grows out of the counter's side.
     */
    private static void banner(GuiGraphicsExtractor graphics, Font font, int right, int counterY, long now) {
        float alpha = RubyTickerAnimation.alpha(now);
        int argb = ARGB.color(Math.round(alpha * BANNER_ALPHA * 255), 0xFFFFFF);
        if (ARGB.alpha(argb) < 5) {
            return;
        }
        Component banner = RubyCounter.hudBanner(RubyTickerAnimation.sign(), RubyTickerAnimation.bannerAmount());
        float scale = RubyTickerAnimation.scale(now);
        graphics.pose().pushMatrix();
        graphics.pose().translate(right, counterY - BANNER_RISE + BANNER_MIDDLE - RubyTickerAnimation.rise(now));
        graphics.pose().scale(scale, scale);
        graphics.text(font, banner, -font.width(banner), -BANNER_MIDDLE, argb, true);
        graphics.pose().popMatrix();
    }
}
