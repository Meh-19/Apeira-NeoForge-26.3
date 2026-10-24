package com.apeira.main.realm;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * Origin Realms' chat line icons, glyphs of its {@code minecraft:default} font that Apeira keeps in
 * {@code apeira:chat_icons} ({@code font/chat_icons.json}, the server pack's {@code custom_ui/error_*.png}, 7x7 at ascent
 * 7) so the default font stays vanilla's. A line is the icon in white (its own colours), a space and the coloured text.
 */
public final class ChatIcons {
    /** The red error mark, {@code error_red.png}. */
    public static final String ERROR = "ꑜ";
    /** The green tick, {@code error_green.png}. */
    public static final String SUCCESS = "ꑠ";

    private static final FontDescription FONT = new FontDescription.Resource(ApeiraIds.id("chat_icons"));

    private ChatIcons() {}

    public static MutableComponent icon(String glyph) {
        return Component.literal(glyph).withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE));
    }

    /** {@code icon} then the {@code message.apeira.<key>} line in {@code color}. */
    public static MutableComponent line(String glyph, String key, ChatFormatting color, Object... args) {
        return Component.empty().append(icon(glyph)).append(Component.literal(" ").withStyle(color))
                .append(Component.translatable("message.apeira." + key, args).withStyle(color));
    }
}
