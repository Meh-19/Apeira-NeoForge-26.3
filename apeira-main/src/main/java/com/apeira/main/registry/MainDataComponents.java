package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.item.CapturedMob;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The main module's item data components. */
public final class MainDataComponents {
    /** The mob in a capture net, saved and synced so the net's model shows it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapturedMob>> CAPTURED_MOB = Apeira.REGISTRAR.dataComponent(
            "captured_mob", builder -> builder.persistent(CapturedMob.CODEC).networkSynchronized(CapturedMob.STREAM_CODEC));

    private MainDataComponents() {}

    public static void init(ApeiraRegistrar registrar) {}
}
