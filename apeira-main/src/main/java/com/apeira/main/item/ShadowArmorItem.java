package com.apeira.main.item;

import java.util.function.Consumer;

import com.apeira.main.effect.ShadowModeManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A piece of Xenon's shadow armour. {@link ShadowModeManager} counts the pieces a player wears by this class: each cuts
 * mob detection range, and a full set enters Shadow Mode when hit.
 */
public class ShadowArmorItem extends Item {
    public ShadowArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.apeira.shadow_gear").withStyle(ChatFormatting.DARK_PURPLE));
        builder.accept(Component.translatable("tooltip.apeira.shadow_armor.detection", (int) ShadowModeManager.DETECTION_REDUCTION_PER_PIECE)
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.apeira.shadow_armor.set").withStyle(ChatFormatting.DARK_GRAY));
    }
}
