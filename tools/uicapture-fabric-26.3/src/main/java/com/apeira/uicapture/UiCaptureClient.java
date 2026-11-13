package com.apeira.uicapture;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
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
 * probe's completion requests, so a plain Fabric 26.3 client with only this jar and Fabric API can join a server without Apeira.
 */
public final class UiCaptureClient implements ClientModInitializer {
    public static final String MOD_ID = "apeira_uicapture";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final KeyMapping CAPTURE = new KeyMapping("key.apeira.capture_ui", InputConstants.KEY_F8, KeyMapping.Category.MISC);
    public static final KeyMapping INSPECT = new KeyMapping("key.apeira.inspect_ui", InputConstants.KEY_F9, KeyMapping.Category.MISC);
    public static final KeyMapping LOG_HUD = new KeyMapping("key.apeira.log_hud", InputConstants.KEY_F10, KeyMapping.Category.MISC);
    public static final KeyMapping LOG_CHAT = new KeyMapping("key.apeira.log_chat", InputConstants.KEY_F7, KeyMapping.Category.MISC);

    /** What the next frame should capture: a screen, a screen with a deep inspection, the HUD, the HUD with its text log, or nothing. */
    private enum Pending { NONE, SCREEN, INSPECT, HUD, HUD_LOG }

    private static Pending pending = Pending.NONE;

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(CAPTURE);
        KeyMappingHelper.registerKeyMapping(INSPECT);
        KeyMappingHelper.registerKeyMapping(LOG_HUD);
        KeyMappingHelper.registerKeyMapping(LOG_CHAT);
        ClientTickEvents.END_CLIENT_TICK.register(UiCaptureClient::onClientTick);
        // Commands going to the server; client commands are not included.
        ClientSendMessageEvents.COMMAND.register(CommandLogger::ran);
        // Both fire on the client when the player attacks or uses an entity; the recorder keeps only client-side ones.
        AttackEntityCallback.EVENT.register(ArmorStandRecorder::onAttack);
        UseEntityCallback.EVENT.register(ArmorStandRecorder::onUse);
        CommandLogger.registerVanillaSnapshot();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            SoundInspector.register(dispatcher);
            CommandLogger.register(dispatcher);
            ArmorStandRecorder.register(dispatcher);
        });
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            // Key mappings are not clicked while a screen is open, so the screen's own key event arms the capture.
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> onScreenKey(minecraft, event));
            ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                if (pending == Pending.SCREEN || pending == Pending.INSPECT) {
                    UiCapture.begin();
                }
            });
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                if ((pending == Pending.SCREEN || pending == Pending.INSPECT) && UiCapture.recording()) {
                    finish(s, pending == Pending.INSPECT ? new UiCaptureWriter.Mouse(mouseX, mouseY) : null);
                }
            });
        });
        // The key's display name asks the window system, which is not initialised yet, so log the key's id instead.
        LOGGER.info("{} loaded: press {} to capture a screen", MOD_ID, CAPTURE.getDefaultKey().getName());
    }

    /** A key pressed inside a screen: false when it was one of the capture keys, so the screen does not see it. */
    private static boolean onScreenKey(Minecraft minecraft, KeyEvent event) {
        if (CAPTURE.matches(event)) {
            pending = Pending.SCREEN;
            return false;
        }
        if (INSPECT.matches(event)) {
            pending = Pending.INSPECT;
            return false;
        }
        if (LOG_CHAT.matches(event)) {
            ChatCapture.toggle(minecraft);
            return false;
        }
        if (LOG_HUD.matches(event)) {
            minecraft.gui.hud.getChat().addClientSystemMessage(Component.translatable("message.apeira.ui_capture.hud_needs_no_screen"));
            return false;
        }
        return true;
    }

    private static void onClientTick(Minecraft minecraft) {
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

    /** The start of a HUD frame ({@code mixin/HudMixin}). */
    public static void beginHud() {
        if (pending == Pending.HUD || pending == Pending.HUD_LOG) {
            UiCapture.begin();
        }
    }

    /** The end of a HUD frame ({@code mixin/HudMixin}). */
    public static void endHud() {
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
