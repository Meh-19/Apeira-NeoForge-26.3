package com.apeira.dev;

import org.slf4j.Logger;

import com.apeira.core.network.ApeiraNetwork;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.furniture.FurnitureTunings;
import com.apeira.dev.network.DevPayloads;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Entrypoint for Apeira Dev, the dev-only tools: the furniture dev wand, which tunes decor furniture live
 * ({@link FurnitureTunings}). apeira-main loads it in its runs only, so it never ships with the content mods.
 */
@Mod(ApeiraDev.MOD_ID)
public final class ApeiraDev {
    public static final String MOD_ID = "apeira_dev";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Owns the dev wand, in the shared {@code apeira} namespace. */
    public static final ApeiraRegistrar REGISTRAR = ApeiraRegistrar.create();
    /** The dev module's payloads; bump the version when one of their codecs changes. */
    public static final ApeiraNetwork NETWORK = ApeiraNetwork.create("1");

    public ApeiraDev(IEventBus modEventBus, ModContainer modContainer) {
        // Before blocks cache their shapes, so furniture reads tuned shapes live.
        FurnitureTunings.enable();
        REGISTRAR.register(modEventBus, DevItems::init);
        NETWORK.register(modEventBus, DevPayloads::init);
        modEventBus.addListener(ApeiraDev::addToCreativeTab);
        NeoForge.EVENT_BUS.addListener(TuningSync::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(TuningSync::onServerStopped);
        LOGGER.info("{} initialized: furniture tuning is on", MOD_ID);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS && event.hasPermissions()) {
            event.accept(DevItems.FURNITURE_DEV_WAND);
        }
    }
}
