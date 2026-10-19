package com.apeira.main.client;

import com.apeira.main.Apeira;
import com.apeira.main.registry.MainBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;

/** Client entrypoint for the main module. */
@Mod(value = Apeira.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraClient {
    public ApeiraClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraClient::registerRenderers);
        modEventBus.addListener(ApeiraClient::registerItemProperties);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MainBlockEntities.ECHO_GLASS.get(), EchoGlassRenderer::new);
    }

    private static void registerItemProperties(RegisterSelectItemModelPropertyEvent event) {
        event.register(Apeira.REGISTRAR.id("captured_mob"), CapturedMobProperty.TYPE);
    }
}
