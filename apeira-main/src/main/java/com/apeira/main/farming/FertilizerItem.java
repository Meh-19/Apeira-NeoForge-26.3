package com.apeira.main.farming;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A {@link Fertilizer}: right-clicked on a planter box, or put in its fertilizer slot. */
public class FertilizerItem extends Item {
    private final Fertilizer fertilizer;

    public FertilizerItem(Item.Properties properties, Fertilizer fertilizer) {
        super(properties);
        this.fertilizer = fertilizer;
    }

    public Fertilizer fertilizer() {
        return fertilizer;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        FarmingLore.describe(builder, fertilizer.translationKey() + ".description", 2);
    }
}
