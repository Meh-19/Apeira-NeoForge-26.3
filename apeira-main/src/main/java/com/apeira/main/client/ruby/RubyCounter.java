package com.apeira.main.client.ruby;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.ruby.Rubies;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Origin Realms' ruby counters, written in the server pack's digit fonts as the server writes them: {@code apeira:ruby_hud}
 * (the HUD's {@code ruby_counter}: digits and comma with a dark outline, packed one pixel closer with its E001 space,
 * then E002 (+1) and the E000 ruby icon) and {@code apeira:ruby_menu} (the deposit menu's {@code ruby_gui_numbers}, white
 * with a black outline). Each font draws its glyphs from the top of the line.
 */
public final class RubyCounter {
    private static final Style HUD = Style.EMPTY.withFont(new FontDescription.Resource(ApeiraIds.id("ruby_hud")));
    private static final Style MENU = Style.EMPTY.withFont(new FontDescription.Resource(ApeiraIds.id("ruby_menu")));
    private static final String TIGHTEN = "";
    private static final String GAP = "";
    private static final String ICON = "";

    private RubyCounter() {}

    /** The HUD's digits ("30,400" packed tight), without the icon. */
    public static Component hudDigits(long rubies) {
        return Component.literal(String.join(TIGHTEN, Rubies.format(rubies).split(""))).withStyle(HUD);
    }

    /** The HUD's one-pixel gap and ruby icon, drawn after {@link #hudDigits}. */
    public static MutableComponent hudIcon() {
        return Component.literal(GAP + ICON).withStyle(HUD);
    }

    /**
     * The ruby ticker's banner ({@link RubyTickerAnimation}): a green "+" or red "-" in the default font, then the amount
     * and the ruby icon in the HUD font, as the counter writes them ("+ 250 [ruby]").
     */
    public static Component hudBanner(int sign, long rubies) {
        Component mark = Component.literal(sign < 0 ? "-" : "+").withColor(sign < 0 ? 0xF17F61 : 0x85CC16);
        return Component.empty().append(mark).append(Component.literal(GAP + GAP + GAP).withStyle(HUD))
                .append(hudDigits(rubies)).append(hudIcon());
    }

    /** The deposit menu's balance ("30,400"). */
    public static Component menu(long rubies) {
        return Component.literal(Rubies.format(rubies)).withStyle(MENU);
    }
}
