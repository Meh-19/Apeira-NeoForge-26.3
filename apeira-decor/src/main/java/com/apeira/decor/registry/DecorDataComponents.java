package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Item data components of the decor module. */
public final class DecorDataComponents {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /**
     * {@code apeira:paint}: the paint on an item, a 24-bit RGB value (see {@link com.apeira.decor.paint.Paint}). Saved
     * with the item and sent to clients, which tint the item with it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PAINT = R.dataComponent("paint",
            builder -> builder.persistent(ExtraCodecs.RGB_COLOR_CODEC).networkSynchronized(ByteBufCodecs.INT));

    private DecorDataComponents() {}

    public static void init(ApeiraRegistrar registrar) {}
}
