package com.apeira.decor.furniture;

import java.util.function.Consumer;

import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.multiblock.Multiblock;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A furniture piece's item: placed only where every block of the piece's multiblock footprint is free, with the
 * paintable badge in its tooltip when the piece takes paint. A crate or module "Exclusive" line is the item's
 * {@code minecraft:lore}, set where it is registered.
 */
public class FurnitureBlockItem extends BlockItem {
    public FurnitureBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return super.canPlace(context, state) && Multiblock.canPlace(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        if (getBlock() instanceof FurnitureBlock furniture && furniture.takesPaint()) {
            builder.accept(Component.literal(CosmeticItem.PAINTABLE_BADGE).withStyle(Style.EMPTY.withFont(CosmeticItem.FONT)));
        }
    }
}
