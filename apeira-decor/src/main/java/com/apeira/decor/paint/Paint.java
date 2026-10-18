package com.apeira.decor.paint;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.item.PaintbrushItem;
import com.apeira.decor.registry.DecorDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Paint values and the {@link DecorDataComponents#PAINT apeira:paint} component.
 *
 * <p>A paint value is a 24-bit RGB colour: a {@link PaintColor} shade, or a {@link FinishType}'s reserved value. Items
 * carry it in the {@code apeira:paint} component and painted furniture in its {@link PaintBlockEntity}, which copies it
 * to and from the item when the block is placed, broken or picked.
 */
public final class Paint {
    private Paint() {}

    /** Returns the stack's paint, or {@code null} if it is unpainted. */
    public static @Nullable Integer get(ItemStack stack) {
        return stack.get(DecorDataComponents.PAINT.get());
    }

    /** Paints the stack. */
    public static void set(ItemStack stack, int paint) {
        stack.set(DecorDataComponents.PAINT.get(), paint & 0xFFFFFF);
    }

    /**
     * Whether the painting table accepts the stack: a paintbrush, the item of a {@link PaintableBlock} or a paintable
     * {@link CosmeticItem}.
     */
    public static boolean canPaint(ItemStack stack) {
        return stack.getItem() instanceof PaintbrushItem
                || stack.getItem() instanceof BlockItem item && item.getBlock() instanceof PaintableBlock
                || isPaintableCosmetic(stack);
    }

    /** Whether the stack is a cosmetic that takes paint. */
    public static boolean isPaintableCosmetic(ItemStack stack) {
        return stack.getItem() instanceof CosmeticItem cosmetic && cosmetic.paintable();
    }

    /**
     * The opaque ARGB tint that draws {@code paint}: the colour itself, or a finish's representative colour, since
     * finish effects are not drawn on 26.x yet.
     */
    public static int tint(int paint) {
        FinishType finish = FinishType.fromPaint(paint);
        return ARGB.opaque(finish != null ? finish.color() : paint);
    }

    /**
     * Describes {@code paint} for a tooltip, in its own colour: "Gold Finish" for a finish, "Painted Light Blue,
     * Shade 3" for a palette shade. Returns {@code null} for any other colour, as CosmoLib did.
     */
    public static @Nullable Component describe(int paint) {
        FinishType finish = FinishType.fromPaint(paint);
        if (finish != null) {
            return Component.translatable("tooltip.apeira.paint.finish", Component.translatable(finish.translationKey()))
                    .withStyle(Style.EMPTY.withColor(finish.color()));
        }
        PaintColor.Match match = PaintColor.match(paint);
        if (match != null) {
            return Component.translatable("tooltip.apeira.paint.color", Component.translatable(match.color().translationKey()),
                    match.shade() + 1).withStyle(Style.EMPTY.withColor(paint));
        }
        return null;
    }
}
