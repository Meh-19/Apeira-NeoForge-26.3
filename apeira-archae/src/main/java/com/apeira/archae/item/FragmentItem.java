package com.apeira.archae.item;

import java.util.function.Consumer;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A fragment, which the assembly table puts together into artifacts. Its tooltip shows Argon's fragment badge and how
 * to use it.
 */
public class FragmentItem extends Item {
    /** The font holding the fragment badge ({@code assets/apeira/font/archaeology.json}). */
    public static final FontDescription FONT = new FontDescription.Resource(ApeiraIds.id("archaeology"));
    public static final String FRAGMENT_BADGE = "ꑨ";

    public FragmentItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        builder.accept(Component.literal(FRAGMENT_BADGE).withStyle(Style.EMPTY.withFont(FONT)));
        builder.accept(Component.translatable("tooltip.apeira.assemble_instruction_line1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.apeira.assemble_instruction_line2").withStyle(ChatFormatting.GRAY));
    }
}
