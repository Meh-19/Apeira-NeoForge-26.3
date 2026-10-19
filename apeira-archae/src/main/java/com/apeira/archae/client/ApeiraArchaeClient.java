package com.apeira.archae.client;

import com.apeira.archae.ApeiraArchae;
import com.apeira.archae.assembly.FragmentCostTooltip;
import com.apeira.archae.network.AssemblyRecipesPayload;
import com.apeira.archae.registry.ArchaeBlockEntities;
import com.apeira.archae.registry.ArchaeMenus;
import com.apeira.core.network.client.ApeiraClientNetwork;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client entrypoint for the Apeira Archae module. */
@Mod(value = ApeiraArchae.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraArchaeClient {
    public ApeiraArchaeClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraArchaeClient::registerRenderers);
        modEventBus.addListener(ApeiraArchaeClient::registerScreens);
        modEventBus.addListener(ApeiraArchaeClient::registerTooltips);
        ApeiraClientNetwork.create()
                .handle(AssemblyRecipesPayload.TYPE, AssemblyRecipesPayload::handle)
                .register(modEventBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ArchaeBlockEntities.ARTIFACT_DISPLAY.get(), ArtifactRenderer::new);
        event.registerBlockEntityRenderer(ArchaeBlockEntities.ANIMATED_ARTIFACT.get(), AnimatedArtifactRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ArchaeMenus.ASSEMBLY_TABLE.get(), AssemblyTableScreen::new);
    }

    private static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FragmentCostTooltip.class, ClientFragmentCostTooltip::new);
    }
}
