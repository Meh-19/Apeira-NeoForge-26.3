package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.item.CapturedMob;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The main module's item data components. */
public final class MainDataComponents {
    /** The mob in a capture net, saved and synced so the net's model shows it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapturedMob>> CAPTURED_MOB = Apeira.REGISTRAR.dataComponent(
            "captured_mob", builder -> builder.persistent(CapturedMob.CODEC).networkSynchronized(CapturedMob.STREAM_CODEC));

    /** Ticks of flight left in rocket boots (Xenon's {@code Fuel} custom data), saved and synced for the tooltip. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROCKET_FUEL = Apeira.REGISTRAR.dataComponent(
            "rocket_fuel", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Set once rocket boots have warned that their fuel is low (Xenon's {@code FuelWarned}), so they warn only once. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> ROCKET_FUEL_WARNED = Apeira.REGISTRAR.dataComponent(
            "rocket_fuel_warned", builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    private MainDataComponents() {}

    public static void init(ApeiraRegistrar registrar) {}
}
