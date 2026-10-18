package com.apeira.decor.cosmetic;

import java.util.Locale;

/**
 * A cosmetic's rarity: the coloured name and the rarity badge in front of it. Crates roll rarities with the weights in
 * {@code CrateRarityWeights}.
 */
public enum CosmeticRarity {
    COMMON('ꑲ', 0xAAAAAA),
    RARE('ꑳ', 0x0099DB),
    EPIC('ꑴ', 0x8B5CF6),
    LEGENDARY('ꑵ', 0xF49E0B),
    LIMITED('ꑶ', 0x7EBE1B);

    private final char badge;
    private final int color;

    CosmeticRarity(char badge, int color) {
        this.badge = badge;
        this.color = color;
    }

    /** The character drawing this rarity's badge in the {@link CosmeticItem#FONT cosmetic font}. */
    public char badge() {
        return badge;
    }

    /** The colour of the cosmetic's name (24-bit RGB). */
    public int color() {
        return color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
