package com.apeira.decor.client;

import java.util.List;

import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.paint.Paint;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorEntities;
import com.apeira.decor.registry.DecorMenus;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client entrypoint for the Apeira Decor module. */
@Mod(value = ApeiraDecor.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraDecorClient {
    public ApeiraDecorClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraDecorClient::registerRenderers);
        modEventBus.addListener(ApeiraDecorClient::registerScreens);
        modEventBus.addListener(ApeiraDecorClient::registerBlockTints);
        modEventBus.addListener(ApeiraDecorClient::registerItemTints);
        NeoForge.EVENT_BUS.addListener(ApeiraDecorClient::addPaintTooltip);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Every entity type needs a renderer, even an invisible one: the game crashes rendering an entity without one.
        event.registerEntityRenderer(DecorEntities.SEAT.get(), NoopRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DecorMenus.PAINTING_TABLE.get(), PaintingTableScreen::new);
    }

    /** Paintable blocks: tint index 0 shows the block's paint. */
    private static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(PaintBlockTint.INSTANCE), DecorBlocks.OAK_CHAIR.get());
    }

    /** {@code apeira:paint}, which item definitions use to show an item's paint. Also needed by the data run. */
    private static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ApeiraDecor.REGISTRAR.id("paint"), PaintTintSource.MAP_CODEC);
    }

    /** Names the paint of any painted item, under its name, as CosmoLib did for paintbrushes and furniture. */
    private static void addPaintTooltip(ItemTooltipEvent event) {
        Integer paint = Paint.get(event.getItemStack());
        Component description = paint != null ? Paint.describe(paint) : null;
        if (description != null) {
            List<Component> tooltip = event.getToolTip();
            tooltip.add(Math.min(1, tooltip.size()), description);
        }
    }
}
