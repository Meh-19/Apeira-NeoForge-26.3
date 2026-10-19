package com.apeira.archae.registry;

import com.apeira.archae.ApeiraArchae;
import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Argon's sounds, with Argon's names ({@code assets/apeira/sounds.json}, files under {@code sounds/archaeology/}). */
public final class ArchaeSounds {
    /** Played when an artifact is taken from the assembly table. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TAKE_RESULT = ApeiraArchae.REGISTRAR.forRegistry(Registries.SOUND_EVENT)
            .register("take_result", () -> SoundEvent.createVariableRangeEvent(ApeiraArchae.REGISTRAR.id("take_result")));

    private ArchaeSounds() {}

    public static void init(ApeiraRegistrar registrar) {}
}
