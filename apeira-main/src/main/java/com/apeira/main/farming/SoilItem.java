package com.apeira.main.farming;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Bag O' Soil: right-clicked on a planter box it soils the growing crop, or, with no crop or one already soiled, goes
 * into the box's soil slot for the next crops. A soiled crop harvests at a better star rating and may turn golden.
 * Its picture is the large bag at any count.
 */
public class SoilItem extends Item {
    public SoilItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        FarmingLore.describe(builder, "tooltip.apeira.farming.soil", 2);
    }
}
