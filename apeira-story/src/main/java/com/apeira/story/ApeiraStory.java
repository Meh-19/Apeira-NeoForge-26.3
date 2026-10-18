package com.apeira.story;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Entrypoint for the Apeira Story module. */
@Mod(ApeiraStory.MOD_ID)
public final class ApeiraStory {
    public static final String MOD_ID = "apeira_story";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApeiraStory(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} initialized", MOD_ID);
    }
}
