package com.apeira.decor.cosmetic;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.PaintColor;
import com.apeira.decor.registry.DecorDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A cosmetic, worn in one {@link CosmeticSlot} of the player's wardrobe. Its name is drawn in its rarity's colour
 * behind the rarity badge, and paintable cosmetics show the paintable badge and can be painted in the painting table
 * or with a paintbrush.
 *
 * <p>Its item definition shows a flat rarity token in inventories and the cosmetic's 3-D model when worn (the head
 * and third-person display contexts), with tint index 1 of that model taking the paint (CosmoLib's convention). A
 * cosmetic's default paint, if it has one, is a default {@code apeira:paint} component, so every stack starts with it.
 */
public class CosmeticItem extends Item {
    /** The font holding the rarity and paintable badges ({@code assets/apeira/font/cosmetic.json}). */
    public static final FontDescription FONT = new FontDescription.Resource(ApeiraIds.id("cosmetic"));
    /** The paintable badge in {@link #FONT}. */
    public static final String PAINTABLE_BADGE = "ꑞ";
    /** The colour of the exclusive line (the Crates mod's gold). */
    public static final int EXCLUSIVE_COLOR = 0xFFAA00;

    private final CosmeticSlot slot;
    private final CosmeticRarity rarity;
    private final boolean paintable;
    private final @Nullable Component exclusive;

    /**
     * @param defaultPaint the paint every stack starts with, from {@link #paint(PaintColor, int)} or
     *                     {@link FinishType#paintValue()}, or {@code null} for none
     * @param exclusive    a line naming where the cosmetic comes from ("Aether Crate Exclusive"), drawn in gold under
     *                     the paintable badge, or {@code null} for none
     */
    public CosmeticItem(Item.Properties properties, CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
            @Nullable Integer defaultPaint, @Nullable Component exclusive) {
        super(withDefaultPaint(properties.stacksTo(1), defaultPaint));
        this.slot = slot;
        this.rarity = rarity;
        this.paintable = paintable;
        this.exclusive = exclusive;
    }

    public CosmeticItem(Item.Properties properties, CosmeticSlot slot, CosmeticRarity rarity, boolean paintable,
            @Nullable Integer defaultPaint) {
        this(properties, slot, rarity, paintable, defaultPaint, null);
    }

    private static Item.Properties withDefaultPaint(Item.Properties properties, @Nullable Integer paint) {
        return paint == null ? properties : properties.component(DecorDataComponents.PAINT.get(), paint & 0xFFFFFF);
    }

    /** A palette paint, with {@code shade} counted from 1 (lightest) to 7 (darkest) as CosmoLib's defaults are. */
    public static int paint(PaintColor color, int shade) {
        if (shade < 1 || shade > PaintColor.SHADES) {
            throw new IllegalArgumentException("Shade must be 1-" + PaintColor.SHADES + ", got " + shade);
        }
        return color.shade(shade - 1);
    }

    public CosmeticSlot slot() {
        return slot;
    }

    public CosmeticRarity rarity() {
        return rarity;
    }

    /** Whether the painting table and paintbrush accept this cosmetic. */
    public boolean paintable() {
        return paintable;
    }

    /** The paint every stack of this cosmetic starts with, or {@code null}. */
    public @Nullable Integer defaultPaint() {
        return components().get(DecorDataComponents.PAINT.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.empty()
                .append(Component.literal(String.valueOf(rarity.badge())).withStyle(Style.EMPTY.withFont(FONT)))
                .append(" ")
                .append(super.getName(stack).copy().withStyle(Style.EMPTY.withColor(rarity.color())));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        if (paintable) {
            builder.accept(Component.literal(PAINTABLE_BADGE).withStyle(Style.EMPTY.withFont(FONT)));
        }
        if (exclusive != null) {
            builder.accept(exclusive.copy().withStyle(Style.EMPTY.withColor(EXCLUSIVE_COLOR)));
        }
    }
}
