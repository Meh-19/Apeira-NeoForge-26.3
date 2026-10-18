package com.apeira.decor.paint;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;

/**
 * The painting table's nine paint colours, each with seven shades from lightest to darkest. The shade values are the
 * legacy CosmoLib palette, so paint carried over from it keeps its colour and name.
 */
public enum PaintColor {
    RED(new int[] {13324616, 11293523, 13319210, 11218731, 9900312, 8927807, 7152162}, 3),
    ORANGE(new int[] {16423772, 15238200, 16613660, 15752739, 10511422, 8606770, 8932164}, 2),
    YELLOW(new int[] {16769669, 16762726, 16179281, 16236577, 15180071, 16293157, 12088320}, 3),
    PINK(new int[] {16754910, 16486064, 16737932, 13522360, 11693210, 13649529, 12661577}, 1),
    WHITE(new int[] {16777215, 16777215, 11711154, 10066329, 6710886, 5000268, 3355443}, 0),
    GREEN(new int[] {9681487, 6010202, 7582489, 5994518, 6584908, 5077600, 4347188}, 3),
    PURPLE(new int[] {13330169, 8799149, 13933055, 9597668, 9266071, 9258373, 6109291}, 1),
    BLUE(new int[] {6912482, 3832997, 3565217, 6118815, 5136766, 4279707, 3558760}, 5),
    LIGHT_BLUE(new int[] {11591653, 8509648, 4110557, 7644362, 1481628, 2331003, 5534851}, 2);

    /** Shades per colour. */
    public static final int SHADES = 7;

    private final int[] shades;
    private final int defaultShade;

    PaintColor(int[] shades, int defaultShade) {
        this.shades = shades;
        this.defaultShade = defaultShade;
    }

    /** The shade selected when this colour is picked. */
    public int defaultShade() {
        return defaultShade;
    }

    /** The 24-bit RGB value of shade {@code index}, from 0 (lightest) to 6 (darkest). */
    public int shade(int index) {
        return shades[index];
    }

    /** The translation key of this colour's name, {@code paint_color.apeira.<name>}. */
    public String translationKey() {
        return ApeiraIds.id(name().toLowerCase(Locale.ROOT)).toLanguageKey("paint_color");
    }

    /** A known solid paint shade. */
    public record Match(PaintColor color, int shade) {}

    /** Returns the colour and shade whose RGB value is {@code rgb}, or {@code null} if it is not a palette shade. */
    public static @Nullable Match match(int rgb) {
        for (PaintColor color : values()) {
            for (int i = 0; i < SHADES; i++) {
                if (color.shades[i] == rgb) {
                    return new Match(color, i);
                }
            }
        }
        return null;
    }
}
