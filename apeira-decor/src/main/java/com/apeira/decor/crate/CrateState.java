package com.apeira.decor.crate;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The stage of a crate's opening. The timed stages last as long as the matching animation of the crate's GeckoLib
 * model ({@code animation.<crate>.open}, {@code .reroll} and {@code .vanish}); {@link #LOOP} plays
 * {@code .open_loop} until the opener collects or rerolls.
 */
public enum CrateState implements StringRepresentable {
    /** Placed and waiting to be opened. Only a closed crate can be broken. */
    CLOSED(0),
    /** 3.8 seconds: the lid opens and the three items rise out. */
    OPENING(76),
    /** The items float above the crate until the opener crouches to collect them or punches the crate to reroll. */
    LOOP(0),
    /** 3 seconds: the items sink back and three new ones rise. */
    REROLLING(60),
    /** 0.8 seconds: the crate disappears, then the block is removed. */
    VANISHING(16);

    public static final Codec<CrateState> CODEC = StringRepresentable.fromEnum(CrateState::values);

    private final int ticks;

    CrateState(int ticks) {
        this.ticks = ticks;
    }

    /** How long the stage lasts, or 0 if it has no fixed length. */
    public int ticks() {
        return ticks;
    }

    /** Whether someone is opening the crate: they are protected, and the crate cannot be broken. */
    public boolean inSession() {
        return this == OPENING || this == LOOP || this == REROLLING;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
