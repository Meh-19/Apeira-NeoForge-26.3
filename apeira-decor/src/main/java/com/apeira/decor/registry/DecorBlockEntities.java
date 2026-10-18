package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block entities of the decor module. */
public final class DecorBlockEntities {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** {@code apeira:paint}: the paint of a paintable block. Add every new {@code PaintableBlock} here. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintBlockEntity>> PAINT = R.blockEntityType("paint",
            PaintBlockEntity::new, DecorBlocks.OAK_CHAIR);

    private DecorBlockEntities() {}

    public static void init(ApeiraRegistrar registrar) {}
}
