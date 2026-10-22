package com.apeira.main.client;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.Apeira;
import com.apeira.main.client.entity.CrocodileRenderer;
import com.apeira.main.client.entity.GrizzlyBearRenderer;
import com.apeira.main.client.entity.TurkeyRenderer;
import com.apeira.main.client.entity.WraithRenderer;
import com.apeira.main.client.navigator.ApeiraNavigatorScreen;
import com.apeira.main.network.OpenNavigatorPayload;
import com.apeira.main.registry.MainBlockEntities;
import com.apeira.main.registry.MainEntities;

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
        ApeiraClientNetwork.create()
                .handle(OpenNavigatorPayload.TYPE, ApeiraNavigatorScreen::onOpen)
                .register(modEventBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MainBlockEntities.ECHO_GLASS.get(), EchoGlassRenderer::new);
        event.registerEntityRenderer(MainEntities.TURKEY.get(), TurkeyRenderer::new);
        event.registerEntityRenderer(MainEntities.GRIZZLY_BEAR.get(), GrizzlyBearRenderer::new);
        event.registerEntityRenderer(MainEntities.CROCODILE.get(), CrocodileRenderer::new);
        event.registerEntityRenderer(MainEntities.WRAITH.get(), WraithRenderer::new);
    }

    private static void registerItemProperties(RegisterSelectItemModelPropertyEvent event) {
        event.register(Apeira.REGISTRAR.id("captured_mob"), CapturedMobProperty.TYPE);
    }
}
