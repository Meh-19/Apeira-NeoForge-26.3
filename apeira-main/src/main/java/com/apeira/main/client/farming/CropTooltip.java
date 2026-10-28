package com.apeira.main.client.farming;

import java.util.List;

import com.apeira.main.farming.CropQuality;
import com.apeira.main.farming.FarmingLore;
import com.apeira.main.farming.PlanterBoxBlock;
import com.apeira.main.farming.SprinklerBlock;
import com.apeira.main.registry.MainFarming;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Origin Realms' farming lore that the items can't add themselves, as Ren's inventory captures show it: a rated crop's
 * stars before its name ({@code crop_stars}, no space, "[3 stars]Apple"), the crop badge as the lore of every crop
 * ({@code lore_crop}), and the planter box's and sprinklers' descriptions, which are block items.
 */
public final class CropTooltip {
    private CropTooltip() {}

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }
        int stars = CropQuality.starsOf(stack);
        if (stars > 0) {
            // A sibling, not a child: children take their parent's font.
            tooltip.set(0, Component.empty().append(FarmingLore.glyphs(FarmingLore.STARS[stars - 1])).append(tooltip.get(0)));
        }
        if (stars > 0 || isProduce(stack.getItem())) {
            tooltip.add(1, FarmingLore.glyphs(FarmingLore.BADGE));
        } else if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof PlanterBoxBlock) {
            tooltip.add(1, FarmingLore.line("tooltip.apeira.farming.planter_box"));
        } else if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof SprinklerBlock sprinkler) {
            int size = 2 * sprinkler.radius() + 1;
            tooltip.add(1, FarmingLore.line("tooltip.apeira.farming.sprinkler.1"));
            tooltip.add(2, FarmingLore.line("tooltip.apeira.farming.sprinkler.2", size, size));
        }
    }

    private static boolean isProduce(Item item) {
        return MainFarming.produce().stream().anyMatch(produce -> produce.get() == item)
                || MainFarming.goldenProduce().stream().anyMatch(produce -> produce.get() == item);
    }
}
