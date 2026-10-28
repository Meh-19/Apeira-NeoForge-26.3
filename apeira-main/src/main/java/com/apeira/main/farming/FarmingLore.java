package com.apeira.main.farming;

import java.util.function.Consumer;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * Origin Realms' farming lore, as Ren's inventory captures show it: descriptions in the server's light blue
 * ({@link #DESCRIPTION}, not italic) and icons from the {@code apeira:farming} font ({@code assets/apeira/font/farming.json},
 * the server's {@code custom_ui} glyphs) drawn white.
 */
public final class FarmingLore {
    /** Origin Realms' item description colour. */
    public static final int DESCRIPTION = 0x9FC5F0;
    public static final FontDescription FONT = new FontDescription.Resource(ApeiraIds.id("farming"));
    /** {@code lore_crop}: the crop badge under a crop's name. */
    public static final String BADGE = "";
    /** {@code crop_stars}: one, two and three stars (the sheet's bottom, middle and top rows), before a rated crop's name. */
    public static final String[] STARS = {"", "", ""};
    /** {@code empty_water_drop} and {@code water_drop}: the watering can's water. */
    public static final String EMPTY_DROP = "";
    public static final String DROP = "";

    private FarmingLore() {}

    /** One description line, {@code key} in Origin Realms' description colour. */
    public static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(style -> style.withColor(DESCRIPTION).withItalic(false));
    }

    /** The description lines {@code <key>.1} to {@code <key>.<lines>}. */
    public static void describe(Consumer<Component> builder, String key, int lines) {
        for (int i = 1; i <= lines; i++) {
            builder.accept(line(key + "." + i));
        }
    }

    /** Glyphs of the {@code apeira:farming} font, white. */
    public static MutableComponent glyphs(String glyphs) {
        return Component.literal(glyphs).withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE).withItalic(false));
    }
}
