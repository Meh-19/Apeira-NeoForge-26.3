package com.apeira.main.farming;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A planter box seed. It is a plain item, not a block item, so it cannot be planted on farmland or anywhere else: only
 * a {@link PlanterBoxBlock} takes it (right-clicked on the box, or put in the box's seed slot). Vanilla seeds keep
 * their vanilla planting on farmland.
 */
public class FarmSeedItem extends Item {
    private final FarmCrop crop;

    public FarmSeedItem(Item.Properties properties, FarmCrop crop) {
        super(properties);
        this.crop = crop;
    }

    public FarmCrop crop() {
        return crop;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        // Origin Realms' seeds carry only the crop badge, as their crops do.
        builder.accept(FarmingLore.glyphs(FarmingLore.BADGE));
    }
}
