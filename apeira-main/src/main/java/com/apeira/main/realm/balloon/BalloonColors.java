package com.apeira.main.realm.balloon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.PaintColor;

import net.minecraft.network.chat.Component;

/**
 * The colours of the realm settings' balloon colour popup, in its order: unpainted, then each paint table colour at its
 * default shade, in the painting table's order. {@code /realm balloon} takes the same names.
 */
public final class BalloonColors {
    /** Unpainted, then {@link PaintColor}'s default shades; {@code null} is unpainted. */
    private static final List<@Nullable Integer> PALETTE = palette();

    private BalloonColors() {}

    private static List<@Nullable Integer> palette() {
        List<@Nullable Integer> palette = new ArrayList<>();
        palette.add(null);
        for (PaintColor color : PaintColor.values()) {
            palette.add(color.shade(color.defaultShade()));
        }
        return palette;
    }

    /** How many colours the popup offers. */
    public static int size() {
        return PALETTE.size();
    }

    /** The popup's colour {@code index}; 0 is unpainted ({@code null}). */
    public static @Nullable Integer at(int index) {
        return PALETTE.get(index);
    }

    /** The names {@code /realm balloon} takes: {@code none} and the paint colours' ids. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        names.add("none");
        for (PaintColor color : PaintColor.values()) {
            names.add(color.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    /** The palette colour named {@code name}, as {@link #names} lists them. Throws for an unknown name. */
    public static @Nullable Integer byName(String name) {
        int index = names().indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown balloon colour " + name);
        }
        return PALETTE.get(index);
    }

    /** The paint's name: its paint colour, finish or hex code, or "Unpainted". */
    public static Component name(@Nullable Integer paint) {
        if (paint == null) {
            return Component.translatable("message.apeira.realm.balloon.unpainted");
        }
        FinishType finish = FinishType.fromPaint(paint);
        if (finish != null) {
            return Component.translatable(finish.translationKey());
        }
        PaintColor.Match match = PaintColor.match(paint);
        return match != null ? Component.translatable(match.color().translationKey())
                : Component.literal(String.format(Locale.ROOT, "#%06X", Objects.requireNonNull(paint) & 0xFFFFFF));
    }
}
