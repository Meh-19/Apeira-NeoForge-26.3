package com.apeira.main.client;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.Apeira;
import com.apeira.main.client.entity.CrocodileRenderer;
import com.apeira.main.client.entity.GrizzlyBearRenderer;
import com.apeira.main.client.entity.TurkeyRenderer;
import com.apeira.main.client.entity.WraithRenderer;
import com.apeira.main.client.navigator.ApeiraNavigatorScreen;
import com.apeira.main.client.realm.OnboardingScreen;
import com.apeira.main.client.ruby.RubyDepositRenderer;
import com.apeira.main.client.ruby.RubyDepositScreen;
import com.apeira.main.client.ruby.RubyDepositSound;
import com.apeira.main.client.ruby.RubyHud;
import com.apeira.main.registry.MainBlockEntities;
import com.apeira.main.navigator.OpenNavigatorPayload;
import com.apeira.main.navigator.OpenProfilePayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.apeira.main.realm.OpenOnboardingPayload;
import com.apeira.main.realm.RealmCreatedPayload;
import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainRubies;
import com.apeira.main.ruby.RubyDepositBlockEntity;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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

    public ApeiraClient(IEventBus modEventBus) {
        modEventBus.addListener(ApeiraClient::registerRenderers);
        modEventBus.addListener(ApeiraClient::registerItemProperties);
        modEventBus.addListener(ApeiraClient::registerKeys);
        modEventBus.addListener(ApeiraClient::registerScreens);
        modEventBus.addListener(ApeiraClient::registerGuiLayers);
        RubyDepositBlockEntity.ambience = RubyDepositSound::tick;
        NeoForge.EVENT_BUS.addListener(ApeiraClient::openProfile);
        ApeiraClientNetwork.create()
                .handle(OpenNavigatorPayload.TYPE, ApeiraNavigatorScreen::onOpen)
                .handle(OpenOnboardingPayload.TYPE, OnboardingScreen::onOpen)
                .handle(RealmCreatedPayload.TYPE, OnboardingScreen::onCreated)
                .register(modEventBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MainBlockEntities.ECHO_GLASS.get(), EchoGlassRenderer::new);
        event.registerBlockEntityRenderer(MainRubies.RUBY_DEPOSIT_ENTITY.get(), RubyDepositRenderer::new);
        event.registerEntityRenderer(MainEntities.TURKEY.get(), TurkeyRenderer::new);
        event.registerEntityRenderer(MainEntities.GRIZZLY_BEAR.get(), GrizzlyBearRenderer::new);
        event.registerEntityRenderer(MainEntities.CROCODILE.get(), CrocodileRenderer::new);
        event.registerEntityRenderer(MainEntities.WRAITH.get(), WraithRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MainRubies.RUBY_DEPOSIT_MENU.get(), RubyDepositScreen::new);
    }

    /** The ruby counter is drawn with the food bar, over its right end. */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, Apeira.REGISTRAR.id("rubies"), RubyHud::render);
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
