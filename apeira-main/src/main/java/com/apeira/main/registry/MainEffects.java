package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.effect.ShadowModeEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Xenon's mob effects. */
public final class MainEffects {
    /**
     * {@code apeira:shadow_mode}: a marker for {@link com.apeira.main.effect.ShadowModeManager Shadow Mode}, given with
     * {@link MobEffects#INVISIBILITY}; the shadow mixins read it.
     */
    public static final DeferredHolder<MobEffect, MobEffect> SHADOW_MODE = Apeira.REGISTRAR.forRegistry(Registries.MOB_EFFECT)
            .register("shadow_mode", ShadowModeEffect::new);

    private MainEffects() {}

    public static void init(ApeiraRegistrar registrar) {}
}
