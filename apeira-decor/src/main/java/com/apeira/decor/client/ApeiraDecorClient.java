package com.apeira.decor.client;

import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.registry.DecorEntities;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client entrypoint for the Apeira Decor module. */
@Mod(value = ApeiraDecor.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraDecorClient {
    public ApeiraDecorClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraDecorClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Every entity type needs a renderer, even an invisible one: the game crashes rendering an entity without one.
        event.registerEntityRenderer(DecorEntities.SEAT.get(), NoopRenderer::new);
    }
}
