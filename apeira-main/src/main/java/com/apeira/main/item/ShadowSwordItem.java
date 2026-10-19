package com.apeira.main.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Xenon's shadow sword, a sword of the shadow material. It does 1.5 times the damage to a mob that is not targeting its
 * wielder, on top of Shadow Mode's own 1.5 ({@code mixin/ShadowAttackMixin}), and breaks blocks silently like the shadow
 * tools.
 */
public class ShadowSwordItem extends Item {
    public ShadowSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.apeira.shadow_gear").withStyle(ChatFormatting.DARK_PURPLE));
        builder.accept(Component.translatable("tooltip.apeira.shadow_sword.1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.apeira.shadow_sword.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
