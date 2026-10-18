package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.block.entity.EchoGlassBlockEntity;
import com.apeira.main.block.entity.SpikeTrapBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The main module's block entity types. */
public final class MainBlockEntities {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoGlassBlockEntity>> ECHO_GLASS = R.blockEntityType("echo_glass",
            EchoGlassBlockEntity::new, MainSpecialBlocks.ECHO_GLASS);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpikeTrapBlockEntity>> SPIKE_TRAP = R.blockEntityType("spike_trap",
            SpikeTrapBlockEntity::new, MainSpecialBlocks.SPIKE_TRAP);

    private MainBlockEntities() {}

    public static void init(ApeiraRegistrar registrar) {}
}
