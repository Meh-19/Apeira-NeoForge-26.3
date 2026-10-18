package com.apeira.decor.client;

import java.util.List;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.client.crate.CrateCamera;
import com.apeira.decor.client.crate.CrateHud;
import com.apeira.decor.client.crate.CrateRenderer;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.network.CrateOpenPayload;
import com.apeira.decor.network.CrateUnlockPayload;
import com.apeira.decor.network.OpenWardrobePayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.apeira.decor.paint.Paint;
import com.apeira.decor.registry.DecorBlockEntities;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorEntities;
import com.apeira.decor.registry.DecorMenus;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client entrypoint for the Apeira Decor module. */
@Mod(value = ApeiraDecor.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraDecorClient {
    /** Opens the wardrobe; the period key, as in CosmoLib. */
    public static final KeyMapping OPEN_WARDROBE = new KeyMapping("key.apeira.open_wardrobe", InputConstants.KEY_PERIOD,
            KeyMapping.Category.INVENTORY);

    public ApeiraDecorClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraDecorClient::registerRenderers);
        modEventBus.addListener(ApeiraDecorClient::addCosmeticLayers);
        modEventBus.addListener(ApeiraDecorClient::registerRenderStateModifiers);
        modEventBus.addListener(ApeiraDecorClient::registerKeys);
        modEventBus.addListener(ApeiraDecorClient::registerScreens);
        modEventBus.addListener(ApeiraDecorClient::registerBlockTints);
        modEventBus.addListener(ApeiraDecorClient::registerItemTints);
        NeoForge.EVENT_BUS.addListener(ApeiraDecorClient::addPaintTooltip);
        NeoForge.EVENT_BUS.addListener(ApeiraDecorClient::openWardrobe);
        NeoForge.EVENT_BUS.addListener(CrateCamera::onClientTick);
        NeoForge.EVENT_BUS.addListener(CrateCamera::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(CrateCamera::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(CrateHud::onRenderGui);
        ApeiraClientNetwork.create()
                .handle(CrateOpenPayload.TYPE, CrateCamera::onOpen)
                .handle(CrateUnlockPayload.TYPE, CrateCamera::onUnlock)
                .register(modEventBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Every entity type needs a renderer, even an invisible one: the game crashes rendering an entity without one.
        event.registerEntityRenderer(DecorEntities.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(DecorBlockEntities.CRATE.get(), CrateRenderer::new);
    }

    /** Worn cosmetics, on players and mannequins of both skin models. */
    private static void addCosmeticLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> player = event.getPlayerRenderer(skin);
            if (player != null) {
                player.addLayer(new CosmeticLayer(player));
            }
            AvatarRenderer<ClientMannequin> mannequin = event.getMannequinRenderer(skin);
            if (mannequin != null) {
                mannequin.addLayer(new CosmeticLayer(mannequin));
            }
        }
    }

    private static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(CosmeticLayer.MODIFIER);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WARDROBE);
    }

    private static void openWardrobe(ClientTickEvent.Post event) {
        while (OPEN_WARDROBE.consumeClick()) {
            ApeiraClientNetwork.sendToServer(OpenWardrobePayload.INSTANCE);
        }
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DecorMenus.PAINTING_TABLE.get(), PaintingTableScreen::new);
        event.register(DecorMenus.WARDROBE.get(), WardrobeScreen::new);
    }

    /** Paintable blocks: tint index 0 shows the block's paint. */
    private static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(PaintBlockTint.INSTANCE), DecorBlocks.OAK_CHAIR.get());
    }

    /** {@code apeira:paint}, which item definitions use to show an item's paint. Also needed by the data run. */
    private static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ApeiraDecor.REGISTRAR.id("paint"), PaintTintSource.MAP_CODEC);
    }

    /**
     * Names the paint of any painted item, under its name, as CosmoLib did for paintbrushes and furniture. A cosmetic
     * still in its default paint shows no paint line.
     */
    private static void addPaintTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Integer paint = Paint.get(stack);
        if (paint == null || stack.getItem() instanceof CosmeticItem cosmetic && paint.equals(cosmetic.defaultPaint())) {
            return;
        }
        Component description = Paint.describe(paint);
        if (description != null) {
            List<Component> tooltip = event.getToolTip();
            tooltip.add(Math.min(1, tooltip.size()), description);
        }
    }
}
