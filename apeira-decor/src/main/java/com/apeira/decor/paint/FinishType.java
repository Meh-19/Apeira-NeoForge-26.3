package com.apeira.decor.paint;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;

/**
 * The painting table's special finishes. A finish is stored as paint like a colour, under a reserved value: red and
 * blue are 255 and green is {@code (id - 1) * 2}, the legacy CosmoLib encoding, so finish paint carried over from it is
 * still recognised.
 *
 * <p>Each finish is an animated effect, CosmoLib's finish shaders, drawn on painted items by
 * {@link com.apeira.decor.client.FinishRenderTypes}; the shaders tell finishes apart by their {@link #id() id}. Placed
 * blocks show a finish in its {@link #color() representative colour}.
 */
public enum FinishType {
    RAINBOW(1, 0xFF4444),
    GOLD(2, 0xFFD700),
    GALAXY(3, 0x5500CC),
    MOLTEN(4, 0xFF4500),
    BUBBLE(5, 0x00BFFF),
    FLORAL(6, 0xFF80C0),
    MATRIX(7, 0x00FF41),
    CHROME(8, 0xB8B8B8),
    GLITCH(9, 0xFF00CC),
    CUBED(10, 0x80FFEE),
    SCANNER(11, 0x100820),
    PEARL(12, 0xFFF5E0),
    PHANTOM(13, 0x8B00FF),
    SNOW(14, 0xE0F0FF);

    private final int id;
    private final int color;

    FinishType(int id, int color) {
        this.id = id;
        this.color = color;
    }

    public int id() {
        return id;
    }

    /** The finish's representative colour (24-bit RGB), used where its effect is not drawn. */
    public int color() {
        return color;
    }

    /** The paint value that stands for this finish. */
    public int paintValue() {
        return 0xFF00FF | ((id - 1) * 2) << 8;
    }

    /** The translation key of this finish's name, {@code finish.apeira.<name>}. */
    public String translationKey() {
        return ApeiraIds.id(name().toLowerCase(Locale.ROOT)).toLanguageKey("finish");
    }

    /** Returns the finish a paint value stands for, or {@code null} for a plain colour. */
    public static @Nullable FinishType fromPaint(int paint) {
        int green = paint >> 8 & 0xFF;
        if ((paint & 0xFF00FF) != 0xFF00FF || (green & 1) != 0) {
            return null;
        }
        int id = (green >> 1) + 1;
        for (FinishType finish : values()) {
            if (finish.id == id) {
                return finish;
            }
        }
        return null;
    }
}
