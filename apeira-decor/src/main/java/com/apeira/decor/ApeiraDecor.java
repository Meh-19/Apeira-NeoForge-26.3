package com.apeira.decor;

import org.slf4j.Logger;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.datagen.DecorData;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorEntities;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Entrypoint for the Apeira Decor module. */
@Mod(ApeiraDecor.MOD_ID)
public final class ApeiraDecor {
    public static final String MOD_ID = "apeira_decor";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Owns the furniture and seat entries, in the shared {@code apeira} namespace. */
    public static final ApeiraRegistrar REGISTRAR = ApeiraRegistrar.create();
    /** Generates the models, language, loot tables and tags for {@link #REGISTRAR}'s entries (the {@code decorData} run). */
    public static final ApeiraDataGenerators DATA = ApeiraDataGenerators.create(REGISTRAR);

    public ApeiraDecor(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRAR.register(modEventBus, DecorBlocks::init, DecorEntities::init);
        DATA.register(modEventBus, DecorData::init);
        LOGGER.info("{} initialized", MOD_ID);
    }
}
