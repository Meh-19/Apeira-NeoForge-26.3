package com.apeira.decor;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Entrypoint for the Apeira Decor module. */
@Mod(ApeiraDecor.MOD_ID)
public final class ApeiraDecor {
    public static final String MOD_ID = "apeira_decor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApeiraDecor(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} initialized", MOD_ID);
    }
}
