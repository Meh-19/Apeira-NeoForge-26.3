package com.apeira.main;

import org.slf4j.Logger;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.network.ApeiraNetwork;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.datagen.MainData;
import com.apeira.main.entity.MainEntityEvents;
import com.apeira.main.event.BirchSaplingGrowth;
import com.apeira.main.event.CaptureNetEvents;
import com.apeira.main.event.RocketBootsSafety;
import com.apeira.main.event.StickPlacement;
import com.apeira.main.home.HomeCommand;
import com.apeira.main.navigator.NavigatorCommand;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.network.MainPayloads;
import com.apeira.main.realm.DummyPlayers;
import com.apeira.main.realm.RealmCommand;
import com.apeira.main.realm.RealmPool;
import com.apeira.main.realm.RealmProtection;
import com.apeira.main.registry.MainBlockEntities;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCosmetics;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCreativeTabs;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDataComponents;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainEffects;
import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainFeatures;
import com.apeira.main.registry.MainGear;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainMaterials;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainSounds;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Entrypoint for the Apeira module. */
@Mod(Apeira.MOD_ID)
public final class Apeira {
    public static final String MOD_ID = "apeira";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Owns every content register under the {@code apeira} namespace. */
    public static final ApeiraRegistrar REGISTRAR = ApeiraRegistrar.create();
    /** Generates the models, language, recipes, loot tables, tags and data maps for {@link #REGISTRAR}'s entries. */
    public static final ApeiraDataGenerators DATA = ApeiraDataGenerators.create(REGISTRAR);
    /** The main module's payloads; bump the version when one of their codecs changes. */
    public static final ApeiraNetwork NETWORK = ApeiraNetwork.create("1");

    public Apeira(IEventBus modEventBus, ModContainer modContainer) {
        // Content holder classes (blocks, items, ...) are passed here as they are added.
        REGISTRAR.register(modEventBus, MainItems::init, MainBlocks::init, MainWoods::init, MainStones::init, MainLanterns::init,
                MainRocks::init, MainDyedBlocks::init, MainCrystals::init, MainBuildingBlocks::init, MainPlants::init,
                MainSpecialBlocks::init, MainMaterials::init, MainBlockEntities::init, MainDataComponents::init, MainEffects::init,
                MainGear::init, MainSounds::init, MainFeatures::init, MainEntities::init, MainCosmetics::init, MainCrates::init,
                MainCreativeTabs::init);
        NETWORK.register(modEventBus, MainPayloads::init);
        // Data holder classes (models, recipes, ...) are passed here as they are added.
        DATA.register(modEventBus, MainData::init);
        modEventBus.addListener(MainEntityEvents::registerAttributes);
        modEventBus.addListener(MainEntityEvents::registerSpawnPlacements);
        NeoForge.EVENT_BUS.addListener(NavigatorCommand::register);
        NeoForge.EVENT_BUS.addListener(RealmCommand::register);
        NeoForge.EVENT_BUS.addListener(HomeCommand::register);
        NeoForge.EVENT_BUS.addListener(DummyPlayers::register);
        NeoForge.EVENT_BUS.addListener(DummyPlayers::onServerStopping);
        NeoForge.EVENT_BUS.addListener(RealmPool::onServerStarting);
        NeoForge.EVENT_BUS.addListener(RealmPool::onServerStopped);
        NeoForge.EVENT_BUS.addListener(RealmPool::onServerTick);
        NeoForge.EVENT_BUS.addListener(RealmPool::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onInteractEntity);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onAttack);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onTravel);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onServerTick);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(RealmProtection::onChat);
        NeoForge.EVENT_BUS.addListener(NavigatorService::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(NavigatorService::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(NavigatorService::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(NavigatorService::onServerStopped);
        NeoForge.EVENT_BUS.addListener(StickPlacement::onUseItemOnBlock);
        NeoForge.EVENT_BUS.addListener(CaptureNetEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(RocketBootsSafety::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BirchSaplingGrowth::onGrowFeature);
        LOGGER.info("{} initialized", MOD_ID);
    }
}
