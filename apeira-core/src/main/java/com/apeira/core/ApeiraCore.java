package com.apeira.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Entrypoint for the Apeira Core module. */
@Mod(ApeiraCore.MOD_ID)
public final class ApeiraCore {
    public static final String MOD_ID = "apeira_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApeiraCore(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} initialized", MOD_ID);
    }
}
