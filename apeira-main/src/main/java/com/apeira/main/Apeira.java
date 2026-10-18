package com.apeira.main;

import org.slf4j.Logger;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.registry.ApeiraRegistrar;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Entrypoint for the Apeira module. */
@Mod(Apeira.MOD_ID)
public final class Apeira {
    public static final String MOD_ID = "apeira";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Owns every content register under the {@code apeira} namespace. */
    public static final ApeiraRegistrar REGISTRAR = ApeiraRegistrar.create();
    /** Generates the models, language, recipes and data maps for {@link #REGISTRAR}'s entries. */
    public static final ApeiraDataGenerators DATA = ApeiraDataGenerators.create(REGISTRAR);

    public Apeira(IEventBus modEventBus, ModContainer modContainer) {
        // Content holder classes (blocks, items, ...) are passed here as they are added.
        REGISTRAR.register(modEventBus);
        // Data holder classes (models, recipes, ...) are passed here as they are added.
        DATA.register(modEventBus);
        LOGGER.info("{} initialized", MOD_ID);
    }
}
