package com.apeira.main.client.farming;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.farming.CropQuality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Adds a harvested crop's rating under its name, as Origin Realms' lore: the crop badge and its stars, filled up to the
 * rating and empty after, from the {@code apeira:farming} font ({@code assets/apeira/font/farming.json}).
 */
public final class CropTooltip {
    private static final FontDescription FONT = new FontDescription.Resource(ApeiraIds.id("farming"));
    private static final String BADGE = "";
    private static final String STAR = "";
    private static final String EMPTY_STAR = "";

    private CropTooltip() {}

    public static void onTooltip(ItemTooltipEvent event) {
        int stars = CropQuality.starsOf(event.getItemStack());
        if (stars <= 0 || event.getToolTip().isEmpty()) {
            return;
        }
        MutableComponent line = Component.literal(BADGE + " " + STAR.repeat(stars) + EMPTY_STAR.repeat(CropQuality.MAX_STARS - stars))
                .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE));
        event.getToolTip().add(1, line);
    }
}
