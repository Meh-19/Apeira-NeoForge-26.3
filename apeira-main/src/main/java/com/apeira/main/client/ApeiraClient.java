package com.apeira.main.client;

import java.util.List;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.Apeira;
import com.apeira.main.client.entity.CrocodileRenderer;
import com.apeira.main.client.entity.GrizzlyBearRenderer;
import com.apeira.main.client.entity.TurkeyRenderer;
import com.apeira.main.client.entity.WraithRenderer;
import com.apeira.main.client.farming.CropTooltip;
import com.apeira.main.client.farming.PlanterBoxRenderer;
import com.apeira.main.client.farming.PlanterBoxScreen;
import com.apeira.main.client.navigator.ApeiraNavigatorScreen;
import com.apeira.main.client.npc.NpcRenderer;
import com.apeira.main.client.npc.NpcQuestMarkers;
import com.apeira.main.client.npc.NpcAnchorPlacement;
import com.apeira.main.client.npc.NpcClient;
import com.apeira.main.client.npc.NpcDialogueCamera;
import com.apeira.main.client.npc.NpcDialogueScreen;
import com.apeira.main.client.npc.NpcShopScreen;
import com.apeira.main.client.realm.OnboardingScreen;
import com.apeira.main.client.realm.RealmTopBar;
import com.apeira.main.client.realm.SpawnPlacement;
import com.apeira.main.client.realm.StarterBalloonRenderer;
import com.apeira.main.client.ruby.RubyDepositRenderer;
import com.apeira.main.client.ruby.RubyDepositScreen;
import com.apeira.main.client.ruby.RubyHud;
import com.apeira.main.client.ruby.RubyTickerAnimation;
import com.apeira.main.ruby.RubyChangePayload;
import com.apeira.main.registry.MainBlockEntities;
import com.apeira.main.navigator.OpenNavigatorPayload;
import com.apeira.main.navigator.OpenProfilePayload;
import com.apeira.main.npc.NpcAnchorPayload;
import com.apeira.main.npc.NpcDialoguePayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.apeira.main.realm.OpenOnboardingPayload;
import com.apeira.main.realm.RealmCreatedPayload;
import com.apeira.main.realm.RealmHudPayload;
import com.apeira.main.realm.SpawnPlacementPayload;
import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainFarming;
import com.apeira.main.registry.MainNpcs;
import com.apeira.main.registry.MainRealmBlocks;
import com.apeira.main.registry.MainRubies;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Client entrypoint for the main module. */
@Mod(value = Apeira.MOD_ID, dist = Dist.CLIENT)
public final class ApeiraClient {
    /** Opens the profile of the player aimed at, or the player's own; Shift-right-clicking a player does the same. */
    public static final KeyMapping OPEN_PROFILE = new KeyMapping("key.apeira.open_profile", InputConstants.KEY_O, KeyMapping.Category.MULTIPLAYER);

    public ApeiraClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, MainClientConfig.SPEC);
        modEventBus.addListener(ApeiraClient::registerRenderers);
        modEventBus.addListener(ApeiraClient::registerItemProperties);
        modEventBus.addListener(ApeiraClient::registerKeys);
        modEventBus.addListener(ApeiraClient::registerScreens);
        modEventBus.addListener(ApeiraClient::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(ApeiraClient::openProfile);
        SpawnPlacement.CURSOR.register();
        NpcAnchorPlacement.CURSOR.register();
        NeoForge.EVENT_BUS.addListener(RealmTopBar::onLoggingOut);
        NpcClient.init();
        NpcDialogueCamera.register();
        NeoForge.EVENT_BUS.addListener(RubyTickerAnimation::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(RubyDepositScreen::hideHud);
        NeoForge.EVENT_BUS.addListener(CropTooltip::onTooltip);
        ApeiraClientNetwork.create()
                .handle(OpenNavigatorPayload.TYPE, ApeiraNavigatorScreen::onOpen)
                .handle(OpenOnboardingPayload.TYPE, OnboardingScreen::onOpen)
                .handle(RealmCreatedPayload.TYPE, OnboardingScreen::onCreated)
                .handle(SpawnPlacementPayload.TYPE, SpawnPlacement::onStart)
                .handle(NpcAnchorPayload.TYPE, NpcAnchorPlacement::onStart)
                .handle(NpcDialoguePayload.TYPE, NpcDialogueScreen::onOpen)
                .handle(RealmHudPayload.TYPE, RealmTopBar::onSync)
                .handle(RubyChangePayload.TYPE, RubyTickerAnimation::onChange)
                .register(modEventBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MainBlockEntities.ECHO_GLASS.get(), EchoGlassRenderer::new);
        event.registerBlockEntityRenderer(MainRubies.RUBY_DEPOSIT_ENTITY.get(), RubyDepositRenderer::new);
        event.registerBlockEntityRenderer(MainRealmBlocks.STARTER_BALLOON_ENTITY.get(), StarterBalloonRenderer::new);
        event.registerBlockEntityRenderer(MainFarming.PLANTER_BOX_ENTITY.get(), PlanterBoxRenderer::new);
        event.registerEntityRenderer(MainEntities.TURKEY.get(), TurkeyRenderer::new);
        event.registerEntityRenderer(MainEntities.GRIZZLY_BEAR.get(), GrizzlyBearRenderer::new);
        event.registerEntityRenderer(MainEntities.CROCODILE.get(), CrocodileRenderer::new);
        event.registerEntityRenderer(MainEntities.WRAITH.get(), WraithRenderer::new);
        // The balloon conductors share one GeckoLib model and animation, each with his own texture.
        for (var conductor : List.of(MainNpcs.CONDUCTOR_CARLO, MainNpcs.CONDUCTOR_MARCO, MainNpcs.CONDUCTOR_GINO, MainNpcs.CONDUCTOR_ANTONIO)) {
            NpcRenderer.register(event, conductor.get(), Apeira.REGISTRAR.id("balloon_conductor"), Apeira.REGISTRAR.id(conductor.getId().getPath()));
        }
        // Jeckyl and Jacko wear a static ruby icon over their heads instead of the speech bubble.
        event.registerEntityRenderer(MainNpcs.JECKYL.get(), context -> new NpcRenderer(context, Apeira.REGISTRAR.id("jeckyl"),
                Apeira.REGISTRAR.id("jeckyl"), null, "jeckle_head", NpcRenderer.Marker.RUBY, -1.0F));
        event.registerEntityRenderer(MainNpcs.JACKO.get(), context -> new NpcRenderer(context, Apeira.REGISTRAR.id("jacko"),
                Apeira.REGISTRAR.id("jacko"), Apeira.REGISTRAR.id("jacko_night"), "farmer_head", NpcRenderer.Marker.RUBY, -1.0F));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MainRubies.RUBY_DEPOSIT_MENU.get(), RubyDepositScreen::new);
        event.register(MainFarming.PLANTER_BOX_MENU.get(), PlanterBoxScreen::new);
        event.register(MainNpcs.NPC_SHOP_MENU.get(), NpcShopScreen::new);
    }

    /** The ruby counter is drawn with the food bar, over its right end. */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, Apeira.REGISTRAR.id("rubies"), RubyHud::render);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, Apeira.REGISTRAR.id("realm_top_bar"), RealmTopBar::render);
        event.registerAbove(VanillaGuiLayers.CONTEXTUAL_INFO_BAR, Apeira.REGISTRAR.id("npc_quest_markers"), NpcQuestMarkers::render);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Apeira.REGISTRAR.id("spawn_placement"), SpawnPlacement.CURSOR::renderHud);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Apeira.REGISTRAR.id("npc_anchor_placement"), NpcAnchorPlacement.CURSOR::renderHud);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PROFILE);
    }

    private static void openProfile(ClientTickEvent.Post event) {
        while (OPEN_PROFILE.consumeClick()) {
            Entity aimed = Minecraft.getInstance().crosshairPickEntity;
            ApeiraClientNetwork.sendToServer(new OpenProfilePayload(aimed == null ? -1 : aimed.getId()));
        }
    }

    private static void registerItemProperties(RegisterSelectItemModelPropertyEvent event) {
        event.register(Apeira.REGISTRAR.id("captured_mob"), CapturedMobProperty.TYPE);
    }
}
