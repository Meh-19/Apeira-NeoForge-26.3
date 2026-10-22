package com.apeira.main.client.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.OpenNavigatorPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The navigator, Origin Realms' {@code /navigator} menu, drawn natively from its captured textures instead of as a
 * chest of invisible items. Each page's layers and buttons are in {@link NavigatorLayout}; clicking a button sends a
 * {@link NavigatorActionPayload} and the server answers page changes with an {@link OpenNavigatorPayload}.
 */
public final class ApeiraNavigatorScreen extends Screen {
    /** The chest Origin Realms draws the navigator in; keeping its size keeps every layer and slot where it was. */
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 222;
    /** Where a 256x256, ascent-19 title glyph's top edge lands relative to the chest's top. */
    private static final int LAYER_Y = -6;
    private static final int SLOT_SIZE = 18;
    private static final Identifier HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front");

    private NavigatorPage page;
    private NavigatorLayout.Page layout;
    private int leftPos;
    private int topPos;

    public ApeiraNavigatorScreen(NavigatorPage page) {
        super(Component.translatable("screen.apeira.navigator"));
        this.page = page;
        this.layout = NavigatorLayout.of(page);
    }

    /** Shows {@code payload}'s page, in the open navigator if there is one. */
    public static void onOpen(OpenNavigatorPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof ApeiraNavigatorScreen screen) {
            screen.show(payload.page());
        } else {
            minecraft.gui.setScreen(new ApeiraNavigatorScreen(payload.page()));
        }
    }

    private void show(NavigatorPage page) {
        this.page = page;
        this.layout = NavigatorLayout.of(page);
    }

    @Override
    protected void init() {
        leftPos = (width - IMAGE_WIDTH) / 2;
        topPos = (height - IMAGE_HEIGHT) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        for (Identifier layer : layout.layers()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, layer, leftPos, topPos + LAYER_Y, 0.0F, 0.0F, 256, 256, 256, 256);
        }
        int slot = slotAt(mouseX, mouseY);
        NavigatorLayout.Button hovered = buttonAt(slot);
        if (hovered != null) {
            // The vanilla slot highlight Origin Realms shows, on the slot under the mouse.
            int x = slotX(slot) - 4;
            int y = slotY(slot) - 4;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HIGHLIGHT_BACK, x, y, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HIGHLIGHT_FRONT, x, y, 24, 24);
            graphics.setTooltipForNextFrame(font, tooltip(hovered), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        NavigatorLayout.Button button = buttonAt(slotAt(event.x(), event.y()));
        if (button != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            ApeiraClientNetwork.sendToServer(button.action());
            if (button.action().action() != NavigatorActionPayload.Action.OPEN_PAGE) {
                onClose();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<Component> tooltip(NavigatorLayout.Button button) {
        String key = "navigator.apeira." + button.key();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(key));
        if (Language.getInstance().has(key + ".desc")) {
            lines.add(Component.translatable(key + ".desc").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private NavigatorLayout.@Nullable Button buttonAt(int slot) {
        if (slot < 0) {
            return null;
        }
        for (NavigatorLayout.Button button : layout.buttons()) {
            for (int s : button.slots()) {
                if (s == slot) {
                    return button;
                }
            }
        }
        return null;
    }

    /** The chest slot under the mouse, or -1. Slots are 18 pixels apart with a 1-pixel border around each 16-pixel item. */
    private int slotAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - leftPos - 7) / SLOT_SIZE);
        int row = (int) Math.floor((mouseY - topPos - 17) / SLOT_SIZE);
        return column >= 0 && column < 9 && row >= 0 && row < 6 ? row * 9 + column : -1;
    }

    private int slotX(int slot) {
        return leftPos + 8 + slot % 9 * SLOT_SIZE;
    }

    private int slotY(int slot) {
        return topPos + 18 + slot / 9 * SLOT_SIZE;
    }
}
