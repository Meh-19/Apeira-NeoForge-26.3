package com.apeira.decor.furniture;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.Direction;

/**
 * Where a furniture piece may be placed, by the face of the block it is placed against. Pieces take a set of them (a
 * bit mask, {@link #mask(Set)}), such as {@code FLOOR | WALL}.
 */
public enum FurniturePlacement {
    /** On top of a block, turned to face the placing player. */
    FLOOR,
    /** Against the side of a block. */
    WALL,
    /** Under a block. */
    CEILING,
    /** On top of a block, always at the piece's own rotation instead of facing the player. */
    DEFAULT;

    /** Every piece is placed on any face today, so this is a piece's set until it is tuned. */
    public static final int ANY_FACE = FLOOR.bit() | WALL.bit() | CEILING.bit();

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int mask) {
        return (mask & bit()) != 0;
    }

    public static int mask(Set<FurniturePlacement> placements) {
        int mask = 0;
        for (FurniturePlacement placement : placements) {
            mask |= placement.bit();
        }
        return mask;
    }

    public static Set<FurniturePlacement> of(int mask) {
        Set<FurniturePlacement> placements = EnumSet.noneOf(FurniturePlacement.class);
        for (FurniturePlacement placement : values()) {
            if (placement.in(mask)) {
                placements.add(placement);
            }
        }
        return placements;
    }

    /** Whether a piece with {@code mask} may be placed against {@code clickedFace}. */
    public static boolean allows(int mask, Direction clickedFace) {
        return switch (clickedFace) {
            case UP -> FLOOR.in(mask) || DEFAULT.in(mask);
            case DOWN -> CEILING.in(mask);
            default -> WALL.in(mask);
        };
    }
}
