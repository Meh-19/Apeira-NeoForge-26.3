package com.apeira.uicapture;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.google.gson.JsonObject;
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
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Apeira UI Capture, a client-only dev tool: the "Capture UI Layout" key (F8 by default, rebindable in Controls)
 * records the next frame of the open screen, or of the HUD when no screen is open, and writes it with
 * {@link UiCaptureWriter}. The "Deep Inspect UI" key (F9) captures the open screen the same way and adds what is under
 * the mouse: the hovered slot and widgets and the text drawn there and in the tooltip, with its colours, formatting and
 * fonts. "Log HUD Elements" (F10, no screen open) captures the HUD like F8 and adds what it shows as text
 * ({@link HudLog}); "Toggle Chat Logger" (F7, also inside screens) logs chat messages ({@link ChatCapture}).
 * {@code /apeirasound} inspects a sound event and logs played sounds ({@link SoundInspector}); {@code /apeiracommands}
 * logs the server's command tree and, with {@code probe}, the server's completions, and records the commands the player
 * runs ({@link CommandLogger}); {@code /apeiradev area} and {@code /apeiradev record} record the armor stands and other
 * entities in an area as a GeckoLib model and animation ({@link ArmorStandRecorder}). They are client commands, so they
 * work on any server. It works on any screen, container or not: nothing is registered and nothing is sent but a
 * probe's completion requests, so a plain NeoForge client with only this jar can join a server without Apeira.
 */
@Mod(value = UiCaptureClient.MOD_ID, dist = Dist.CLIENT)
public final class UiCaptureClient {
    public static final String MOD_ID = "apeira_uicapture";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final KeyMapping CAPTURE = new KeyMapping("key.apeira.capture_ui", InputConstants.KEY_F8, KeyMapping.Category.MISC);
    public static final KeyMapping INSPECT = new KeyMapping("key.apeira.inspect_ui", InputConstants.KEY_F9, KeyMapping.Category.MISC);
    public static final KeyMapping LOG_HUD = new KeyMapping("key.apeira.log_hud", InputConstants.KEY_F10, KeyMapping.Category.MISC);
    public static final KeyMapping LOG_CHAT = new KeyMapping("key.apeira.log_chat", InputConstants.KEY_F7, KeyMapping.Category.MISC);

    /** What the next frame should capture: a screen, a screen with a deep inspection, the HUD, the HUD with its text log, or nothing. */
    private enum Pending { NONE, SCREEN, INSPECT, HUD, HUD_LOG }

    private static Pending pending = Pending.NONE;

    public UiCaptureClient(IEventBus modEventBus) {
        modEventBus.addListener(UiCaptureClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, UiCaptureClient::onScreenKey);
        NeoForge.EVENT_BUS.addListener(UiCaptureClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onHudRenderPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, UiCaptureClient::onHudRenderPost);
        NeoForge.EVENT_BUS.addListener(UiCaptureClient::registerCommands);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CommandLogger::onRegisterCommands);
        // Both fire on the client when the player attacks or uses an entity; the recorder keeps only client-side ones.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, AttackEntityEvent.class, ArmorStandRecorder::onAttack);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, PlayerInteractEvent.EntityInteract.class, ArmorStandRecorder::onUse);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CAPTURE);
        event.register(INSPECT);
        event.register(LOG_HUD);
        event.register(LOG_CHAT);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        SoundInspector.register(event.getDispatcher());
        CommandLogger.register(event.getDispatcher());
        ArmorStandRecorder.register(event.getDispatcher());
    }

    /** Key mappings are not clicked while a screen is open, so the screen's own key event arms the capture. */
    private static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (CAPTURE.matches(event.getKeyEvent())) {
            pending = Pending.SCREEN;
            event.setCanceled(true);
        } else if (INSPECT.matches(event.getKeyEvent())) {
            pending = Pending.INSPECT;
            event.setCanceled(true);
        } else if (LOG_CHAT.matches(event.getKeyEvent())) {
            ChatCapture.toggle(Minecraft.getInstance());
            event.setCanceled(true);
        } else if (LOG_HUD.matches(event.getKeyEvent())) {
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.translatable("message.apeira.ui_capture.hud_needs_no_screen"));
            event.setCanceled(true);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        CommandLogger.tick(minecraft);
        ArmorStandRecorder.tick(minecraft);
        boolean noScreen = minecraft.gui.screen() == null;
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
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.translatable("message.apeira.ui_capture.inspect_needs_screen"));
            }
        }
        while (LOG_HUD.consumeClick()) {
            if (noScreen) {
                pending = Pending.HUD_LOG;
            }
        }
        while (LOG_CHAT.consumeClick()) {
            ChatCapture.toggle(minecraft);
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
        if (pending == Pending.HUD || pending == Pending.HUD_LOG) {
            UiCapture.begin();
        }
    }

    private static void onHudRenderPost(RenderGuiEvent.Post event) {
        if ((pending == Pending.HUD || pending == Pending.HUD_LOG) && UiCapture.recording()) {
            Minecraft minecraft = Minecraft.getInstance();
            // Collected before the frame ends, so the fonts of its text are described with the drawn ones.
            JsonObject hud = pending == Pending.HUD_LOG ? HudLog.collect(minecraft, UiCaptureWriter.ops(minecraft)) : null;
            finish(null, null, hud);
        }
    }

    private static void finish(@Nullable Screen screen, UiCaptureWriter.@Nullable Mouse mouse) {
        finish(screen, mouse, null);
    }

    private static void finish(@Nullable Screen screen, UiCaptureWriter.@Nullable Mouse mouse, @Nullable JsonObject hud) {
        pending = Pending.NONE;
        UiCapture.Frame frame = UiCapture.end();
        Minecraft minecraft = Minecraft.getInstance();
        Component message;
        try {
            Path file = UiCaptureWriter.write(minecraft, screen, frame, mouse, hud);
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
