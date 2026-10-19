package com.apeira.archae;

import org.slf4j.Logger;

import com.apeira.archae.assembly.AssemblyRecipes;
import com.apeira.archae.datagen.ArchaeData;
import com.apeira.archae.network.ArchaePayloads;
import com.apeira.archae.registry.ArchaeArtifacts;
import com.apeira.archae.registry.ArchaeBlockEntities;
import com.apeira.archae.registry.ArchaeBlocks;
import com.apeira.archae.registry.ArchaeCreativeTabs;
import com.apeira.archae.registry.ArchaeItems;
import com.apeira.archae.registry.ArchaeMenus;
import com.apeira.archae.registry.ArchaeSounds;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.network.ApeiraNetwork;
import com.apeira.core.registry.ApeiraRegistrar;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Entrypoint for the Apeira Archae module: the legacy Argon mod's archaeology. */
@Mod(ApeiraArchae.MOD_ID)
public final class ApeiraArchae {
    public static final String MOD_ID = "apeira_archae";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Owns the fragments, artifacts and assembly table, in the shared {@code apeira} namespace. */
    public static final ApeiraRegistrar REGISTRAR = ApeiraRegistrar.create();
    /** Generates the models, language, loot tables, tags and assembly recipes for {@link #REGISTRAR}'s entries (the {@code archaeData} run). */
    public static final ApeiraDataGenerators DATA = ApeiraDataGenerators.create(REGISTRAR);
    /** The archae module's payloads; bump the version when one of their codecs changes. */
    public static final ApeiraNetwork NETWORK = ApeiraNetwork.create("1");

    public ApeiraArchae(IEventBus modEventBus, ModContainer modContainer) {
        // Fragments come first: artifacts name the fragment they are made from.
        REGISTRAR.register(modEventBus, ArchaeItems::init, ArchaeBlocks::init, ArchaeArtifacts::init, ArchaeBlockEntities::init,
                ArchaeMenus::init, ArchaeSounds::init, ArchaeCreativeTabs::init);
        NETWORK.register(modEventBus, ArchaePayloads::init);
        DATA.register(modEventBus, ArchaeData::init);
        NeoForge.EVENT_BUS.addListener(AssemblyRecipes::addListener);
        NeoForge.EVENT_BUS.addListener(AssemblyRecipes::onDatapackSync);
        LOGGER.info("{} initialized", MOD_ID);
    }
}
