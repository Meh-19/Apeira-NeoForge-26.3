package com.apeira.uicapture;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Apeira UI Capture, a client-only dev tool: the "Capture UI Layout" key (F8 by default, rebindable in Controls)
 * records the next frame of the open screen, or of the HUD when no screen is open, and writes it with
 * {@link UiCaptureWriter}. The "Deep Inspect UI" key (F9) captures the open screen the same way and adds what is under
 * the mouse: the hovered slot and widgets and the text drawn there and in the tooltip, with its colours, formatting and
 * fonts. It works on any screen, container or not, on any server: nothing is registered and nothing
 * is sent, so a plain NeoForge client with only this jar can join a server without Apeira.
 */
@Mod(value = UiCaptureClient.MOD_ID, dist = Dist.CLIENT)
public final class UiCaptureClient {
    public static final String MOD_ID = "apeira_uicapture";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final KeyMapping CAPTURE = new KeyMapping("key.apeira.capture_ui", InputConstants.KEY_F8, KeyMapping.Category.MISC);
    public static final KeyMapping INSPECT = new KeyMapping("key.apeira.inspect_ui", InputConstants.KEY_F9, KeyMapping.Category.MISC);

    /** What the next frame should capture: a screen, a screen with a deep inspection, the HUD, or nothing. */
    private enum Pending { NONE, SCREEN, INSPECT, HUD }

    private static Pending pending = Pending.NONE;

    public UiCaptureClient(IEventBus modEventBus) {
        modEventBus.addListener(UiCaptureClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, UiCaptureClient::onScreenKey);
        NeoForge.EVENT_BUS.addListener(UiCaptureClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onHudRenderPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onHudRenderPost);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CAPTURE);
        event.register(INSPECT);
    }

    /** Key mappings are not clicked while a screen is open, so the screen's own key event arms the capture. */
    private static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (CAPTURE.matches(event.getKeyEvent())) {
            pending = Pending.SCREEN;
            event.setCanceled(true);
        } else if (INSPECT.matches(event.getKeyEvent())) {
            pending = Pending.INSPECT;
            event.setCanceled(true);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        boolean noScreen = Minecraft.getInstance().gui.screen() == null;
        if ((pending == Pending.SCREEN || pending == Pending.INSPECT) && noScreen) {
            // The screen closed before it drew again.
            pending = Pending.NONE;
        }
        while (CAPTURE.consumeClick()) {
            if (noScreen) {
                pending = Pending.HUD;
            }
        }
        while (INSPECT.consumeClick()) {
            if (noScreen) {
                // The HUD has no mouse to inspect under.
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.translatable("message.apeira.ui_capture.inspect_needs_screen"));
            }
        }
    }

    private static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (pending == Pending.SCREEN || pending == Pending.INSPECT) {
            UiCapture.begin();
        }
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if ((pending == Pending.SCREEN || pending == Pending.INSPECT) && UiCapture.recording()) {
            finish(event.getScreen(), pending == Pending.INSPECT ? new UiCaptureWriter.Mouse(event.getMouseX(), event.getMouseY()) : null);
        }
    }

    private static void onHudRenderPre(RenderGuiEvent.Pre event) {
        if (pending == Pending.HUD) {
            UiCapture.begin();
        }
    }

    private static void onHudRenderPost(RenderGuiEvent.Post event) {
        if (pending == Pending.HUD && UiCapture.recording()) {
            finish(null, null);
        }
    }

    private static void finish(@Nullable Screen screen, UiCaptureWriter.@Nullable Mouse mouse) {
        pending = Pending.NONE;
        UiCapture.Frame frame = UiCapture.end();
        Minecraft minecraft = Minecraft.getInstance();
        Component message;
        try {
            Path file = UiCaptureWriter.write(minecraft, screen, frame, mouse);
            LOGGER.info("UI layout captured to {}", file);
            message = Component.translatable("message.apeira.ui_capture.done", file.getFileName().toString());
        } catch (Exception e) {
            LOGGER.error("UI capture failed", e);
            message = Component.translatable("message.apeira.ui_capture.failed", String.valueOf(e.getMessage()));
        }
        SystemToast.add(minecraft.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("Apeira UI Capture"), message);
        minecraft.gui.hud.getChat().addClientSystemMessage(message);
    }
}
