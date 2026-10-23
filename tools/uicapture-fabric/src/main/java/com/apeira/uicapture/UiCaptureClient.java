package com.apeira.uicapture;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Apeira UI Capture for Fabric 1.21.11, the client Origin Realms is played on: the "Capture UI Layout" key (F8 by
 * default, rebindable in Controls) records the next frame of the open screen, or of the HUD when no screen is open, and
 * writes it with {@link UiCaptureWriter}. The "Deep Inspect UI" key (F9) captures the open screen the same way and adds
 * what is under the mouse: the hovered slot and widgets and the text drawn there and in the tooltip, with its colours,
 * formatting and fonts. Nothing is registered and nothing is sent, so it works on any server.
 */
public final class UiCaptureClient implements ClientModInitializer {
    public static final String MOD_ID = "apeira_uicapture";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final KeyMapping CAPTURE = new KeyMapping("key.apeira.capture_ui", InputConstants.KEY_F8, KeyMapping.Category.MISC);
    public static final KeyMapping INSPECT = new KeyMapping("key.apeira.inspect_ui", InputConstants.KEY_F9, KeyMapping.Category.MISC);

    /** What the next frame should capture: a screen, a screen with a deep inspection, the HUD, or nothing. */
    private enum Pending { NONE, SCREEN, INSPECT, HUD }

    private static Pending pending = Pending.NONE;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(CAPTURE);
        KeyBindingHelper.registerKeyBinding(INSPECT);
        ClientTickEvents.END_CLIENT_TICK.register(UiCaptureClient::onClientTick);
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            // Key mappings are not clicked while a screen is open, so the screen's own key event arms the capture.
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                if (CAPTURE.matches(event)) {
                    pending = Pending.SCREEN;
                    return false;
                }
                if (INSPECT.matches(event)) {
                    pending = Pending.INSPECT;
                    return false;
                }
                return true;
            });
            ScreenEvents.beforeRender(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                if (pending == Pending.SCREEN || pending == Pending.INSPECT) {
                    UiCapture.begin();
                }
            });
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                if ((pending == Pending.SCREEN || pending == Pending.INSPECT) && UiCapture.recording()) {
                    finish(s, pending == Pending.INSPECT ? new UiCaptureWriter.Mouse(mouseX, mouseY) : null);
                }
            });
        });
        // The key's display name asks GLFW, which is not initialised yet and would fail the window's startup, so log the
        // key's id instead.
        LOGGER.info("{} loaded: press {} to capture a screen", MOD_ID, CAPTURE.getDefaultKey().getName());
    }

    private static void onClientTick(Minecraft minecraft) {
        boolean noScreen = minecraft.screen == null;
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
                minecraft.gui.getChat().addMessage(Component.translatable("message.apeira.ui_capture.inspect_needs_screen"));
            }
        }
    }

    public static void beginHud() {
        if (pending == Pending.HUD) {
            UiCapture.begin();
        }
    }

    public static void endHud() {
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
        SystemToast.add(minecraft.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("Apeira UI Capture"), message);
        minecraft.gui.getChat().addMessage(message);
    }
}
