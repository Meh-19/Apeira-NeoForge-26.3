package com.apeira.archae.item;

import java.util.function.Consumer;

import com.apeira.archae.block.ArtifactBlock;
import com.apeira.decor.cosmetic.CosmeticItem;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * An artifact's item. Its tooltip shows the paintable badge for a paintable artifact and Argon's gold "Archeology
 * Exclusive" line.
 */
public class ArtifactBlockItem extends BlockItem {
    public ArtifactBlockItem(ArtifactBlock block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        if (getBlock() instanceof ArtifactBlock artifact && artifact.takesPaint()) {
            builder.accept(Component.literal(CosmeticItem.PAINTABLE_BADGE).withStyle(Style.EMPTY.withFont(CosmeticItem.FONT)));
        }
        builder.accept(Component.translatable("tooltip.apeira.exclusive.archaeology").withStyle(Style.EMPTY.withColor(CosmeticItem.EXCLUSIVE_COLOR)));
    }
}
