package com.apeira.main.client.crate;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * The Badger Crate opening's sounds, the server pack's {@code stereo/crates/} files ({@code sounds/badger_crate/}),
 * played when the animation reaches the times of the sound keyframes in Ren's Blockbench file (its Effects track):
 * the landing at 0 s, the opening at 0.75 s, three flights from 4.55 s, the idle hum at 5.55 s and three hits from
 * 6.3 s. They are client-only events, as only the opener hears them.
 */
final class BadgerCrateSounds {
    /** Ticks into the animation and the sound played then. */
    private static final int[] TICKS = {0, 15, 91, 98, 105, 111, 126, 133, 139};
    private static final String[] NAMES = {"land", "open_no_land", "fly_1", "fly_2", "fly_3", "idle", "hit_1", "hit_2", "hit_3"};
    private static final SoundEvent[] EVENTS = new SoundEvent[NAMES.length];

    static {
        for (int i = 0; i < NAMES.length; i++) {
            EVENTS[i] = SoundEvent.createVariableRangeEvent(ApeiraIds.id("badger_crate." + NAMES[i]));
        }
    }

    private BadgerCrateSounds() {}

    /** Plays every sound whose time is after {@code fromTick} and at or before {@code toTick}. */
    static void play(int fromTick, int toTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        for (int i = 0; i < TICKS.length; i++) {
            if (TICKS[i] > fromTick && TICKS[i] <= toTick) {
                minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ(), EVENTS[i],
                        SoundSource.MASTER, 1.0F, 1.0F, false);
            }
        }
    }
}
