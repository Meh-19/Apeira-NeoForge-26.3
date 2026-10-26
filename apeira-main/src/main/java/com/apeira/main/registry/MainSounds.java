package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Xenon's mob and biome sounds, with Xenon's names, and the ruby sounds ({@code assets/apeira/sounds.json}). */
public final class MainSounds {
    public static final DeferredHolder<SoundEvent, SoundEvent> TURKEY_IDLE_1 = sound("entity.turkey.idle1");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURKEY_IDLE_2 = sound("entity.turkey.idle2");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURKEY_HURT = sound("entity.turkey.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRIZZLY_BEAR_ROAR = sound("entity.grizzly_bear.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_IDLE = sound("entity.wraith.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_TARGET = sound("entity.wraith.target");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_HURT = sound("entity.wraith.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_DEATH = sound("entity.wraith.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_BITE = sound("entity.wraith.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_APPARATE = sound("entity.wraith.apparate");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRAITH_APPEAR = sound("entity.wraith.appear");
    /** The Depths' ambient loop: the basalt deltas' ambience, slowed down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPTHS_AMBIENT = sound("ambient.depths");
    /** Played to a player when rubies are added to their balance (Origin Realms' {@code ruby-gain}). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUBY_GAIN = sound("ui.ruby_gain");

    private MainSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return Apeira.REGISTRAR.forRegistry(Registries.SOUND_EVENT).register(name,
                () -> SoundEvent.createVariableRangeEvent(Apeira.REGISTRAR.id(name)));
    }

    public static void init(ApeiraRegistrar registrar) {}
}
