package com.apeira.main.farming;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Origin Realms' fertilizers. A crop takes one, of one type; the planter box keeps a stack of one type for the next
 * crops. Each lights its own icon in the planter box menu ({@code textures/gui/farming/menu_fert_<icon>.png}).
 */
public enum Fertilizer {
    /** Doubles or triples the harvest (2 or 3 crops, half the time each: 2.5 on average). */
    PRODUCE_MULTIPLIER("multiplier"),
    /** The crop is ripe at its first watered morning, whatever its growth time. */
    SPEED_GROW("speed"),
    /** The harvest is 3-star, whatever the soil and tool. */
    THREE_STAR_GUARANTEE("star"),
    /** The crop turns golden when it ripens, whatever the soil. */
    GOLDEN_GOODS("gold");

    private static final Fertilizer[] VALUES = values();

    private final String icon;

    Fertilizer(String icon) {
        this.icon = icon;
    }

    /** The menu overlay that lights this fertilizer's icon. */
    public Identifier menuOverlay() {
        return ApeiraIds.id("textures/gui/farming/menu_fert_" + icon + ".png");
    }

    public String translationKey() {
        return "fertilizer.apeira." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The fertilizer an item is, or {@code null}. */
    public static @Nullable Fertilizer of(ItemStack stack) {
        return stack.getItem() instanceof FertilizerItem fertilizer ? fertilizer.fertilizer() : null;
    }

    public static @Nullable Fertilizer byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
