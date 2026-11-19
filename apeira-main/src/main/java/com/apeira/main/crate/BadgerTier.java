package com.apeira.main.crate;

import java.util.Locale;

/**
 * A reward's rarity tier. Each binds the opening's VFX textures of its colour
 * ({@code textures/entity/badger_crate/vfx/<colour>_glow}, {@code _trail}, {@code _pulse}, {@code <colour>_spark/}) to
 * the reward it was rolled for.
 */
public enum BadgerTier {
    COMMON("white", 55),
    RARE("blue", 27),
    EPIC("purple", 13),
    LEGENDARY("yellow", 5);

    private final String colour;
    private final int weight;

    BadgerTier(String colour, int weight) {
        this.colour = colour;
        this.weight = weight;
    }

    /** The VFX texture colour: white, blue, purple or yellow. */
    public String colour() {
        return colour;
    }

    /**
     * The tier's chance in percent, shared by every crate (55/27/13/5); a crate's items of one tier split it evenly
     * ({@link BadgerLoot#chance}).
     */
    public int weight() {
        return weight;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BadgerTier byId(int ordinal) {
        BadgerTier[] tiers = values();
        return tiers[Math.floorMod(ordinal, tiers.length)];
    }
}
