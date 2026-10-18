package com.apeira.decor;

import org.slf4j.Logger;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.network.ApeiraNetwork;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.datagen.DecorData;
import com.apeira.decor.network.DecorPayloads;
import com.apeira.decor.registry.DecorAttachments;
import com.apeira.decor.registry.DecorBlockEntities;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorDataComponents;
import com.apeira.decor.registry.DecorEntities;
import com.apeira.decor.registry.DecorItems;
import com.apeira.decor.registry.DecorMenus;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Entrypoint for the Apeira Decor module. */
@Mod(ApeiraDecor.MOD_ID)
public final class ApeiraDecor {
    public static final String MOD_ID = "apeira_decor";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Owns the furniture, painting, cosmetic and seat entries, in the shared {@code apeira} namespace. */
    public static final ApeiraRegistrar REGISTRAR = ApeiraRegistrar.create();
    /** Generates the models, language, loot tables and tags for {@link #REGISTRAR}'s entries (the {@code decorData} run). */
    public static final ApeiraDataGenerators DATA = ApeiraDataGenerators.create(REGISTRAR);
    /** The decor module's payloads; bump the version when one of their codecs changes. */
    public static final ApeiraNetwork NETWORK = ApeiraNetwork.create("1");

    public ApeiraDecor(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRAR.register(modEventBus, DecorDataComponents::init, DecorBlocks::init, DecorItems::init, DecorBlockEntities::init,
                DecorEntities::init, DecorMenus::init, DecorAttachments::init);
        NETWORK.register(modEventBus, DecorPayloads::init);
        DATA.register(modEventBus, DecorData::init);
        LOGGER.info("{} initialized", MOD_ID);
    }
}
