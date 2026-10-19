package com.apeira.dev;

import com.apeira.decor.furniture.FurnitureBlock;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The furniture dev wand ({@code apeira:furniture_dev_wand}). Right-clicking a furniture piece binds the client's tuning
 * session to it ({@code client/DevSession}, which listens for the click); while it is held, the HUD shows the piece's
 * {@link com.apeira.decor.furniture.FurnitureTuning}, Ctrl + scroll changes the selected value, Alt + scroll picks
 * another, and the editor key opens the editor and exporter.
 *
 * <p>The wand acts before the block's own right-click (NeoForge's {@code onItemUseFirst}), so binding a chair does not
 * sit the player down.
 */
public class FurnitureDevWandItem extends Item {
    public FurnitureDevWandItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof FurnitureBlock
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }
}
