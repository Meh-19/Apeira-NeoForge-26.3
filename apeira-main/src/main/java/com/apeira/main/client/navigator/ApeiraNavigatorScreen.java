package com.apeira.main.client.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.navigator.NavigatorView;
import com.apeira.main.navigator.OpenNavigatorPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The navigator, Origin Realms' {@code /navigator} menu with its Realms pages, drawn natively from the captured
 * textures instead of as a chest of invisible items. The server sends each page as a {@link NavigatorView}; this screen
 * draws its layers and buttons ({@link NavigatorLayout}), the list's player heads with their tooltips and the page
 * number, and sends every click as a {@link NavigatorActionPayload}. It changes nothing itself: the server answers
 * each click with the next view, or closes the screen.
 */
public final class ApeiraNavigatorScreen extends Screen {
    /** The chest Origin Realms draws the navigator in; keeping its size keeps every layer and slot where it was. */
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 222;
    /** Where a 256x256, ascent-19 title glyph's top edge lands relative to the chest's top. */
    private static final int LAYER_Y = -6;
    private static final int SLOT_SIZE = 18;
    /** The white of vanilla's slot highlight, at full strength. */
    private static final int HIGHLIGHT_ALPHA = 0x60;
    /** How long a button's highlight takes to fade in, in milliseconds. */
    private static final long HIGHLIGHT_FADE_MS = 120;
    /** The page number, "1/17": centred between the page arrows, in Origin Realms' page text colour. */
    private static final int PAGE_TEXT_X = 70;
    private static final int PAGE_TEXT_Y = 115;
    private static final int PAGE_TEXT_COLOR = 0xFFC6C6C6;

    private NavigatorView view;
    private NavigatorLayout.Page layout;
    private int leftPos;
    private int topPos;
    private int @Nullable [] hovered;
    private long hoverStart;

    public ApeiraNavigatorScreen(NavigatorView view) {
        super(Component.translatable("screen.apeira.navigator"));
        this.view = view;
        this.layout = NavigatorLayout.of(view);
    }

    /** Shows {@code payload}'s view, in the open navigator if there is one, or closes the navigator. */
    public static void onOpen(OpenNavigatorPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        NavigatorView view = payload.view();
        if (minecraft.gui.screen() instanceof ApeiraNavigatorScreen screen) {
            if (view.page() == NavigatorPage.CLOSED) {
                screen.onClose();
            } else {
                screen.show(view);
            }
        } else if (view.page() != NavigatorPage.CLOSED) {
            minecraft.gui.setScreen(new ApeiraNavigatorScreen(view));
        }
    }

    private void show(NavigatorView view) {
        this.view = view;
        this.layout = NavigatorLayout.of(view);
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
        if (layout.showsList()) {
            for (int i = 0; i < view.entries().size(); i++) {
                head(graphics, view.entries().get(i), NavigatorLayout.FIRST_ENTRY + i);
            }
            String pageText = (view.pageIndex() + 1) + "/" + view.pageCount();
            graphics.text(font, pageText, leftPos + PAGE_TEXT_X - font.width(pageText) / 2, topPos + PAGE_TEXT_Y, PAGE_TEXT_COLOR, false);
        }
        view.target().ifPresent(target -> head(graphics, target, NavigatorLayout.POPUP_HEAD));

        int slot = slotAt(mouseX, mouseY);
        NavigatorLayout.Button button = buttonAt(slot);
        NavigatorView.Entry entry = button == null ? entryAt(slot) : null;
        int[] shape = button != null ? button.slots() : entry != null ? new int[] {slot} : null;
        if (!sameShape(shape, hovered)) {
            hovered = shape;
            hoverStart = Util.getMillis();
        }
        if (shape != null) {
            // The whole button lights up, fading in, instead of only the chest slot under the mouse: each of its slots'
            // 18-pixel cells is filled, so the cells join into the button's own shape.
            float fade = Math.min(1.0F, (Util.getMillis() - hoverStart) / (float) HIGHLIGHT_FADE_MS);
            int color = ARGB.color(Math.round(HIGHLIGHT_ALPHA * fade), 0xFFFFFF);
            for (int s : shape) {
                int x = slotX(s) - 1;
                int y = slotY(s) - 1;
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
            }
            List<Component> tooltip = button != null ? button.tooltip() : entryTooltip(entry);
            graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private void head(GuiGraphicsExtractor graphics, NavigatorView.Entry entry, int slot) {
        PlayerFaceExtractor.extractRenderState(graphics, ResolvableProfile.createUnresolved(entry.id()), slotX(slot), slotY(slot), 16);
    }

    private static List<Component> entryTooltip(NavigatorView.Entry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.name()).withColor(NavigatorService.LABEL));
        lines.addAll(entry.lines());
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int slot = slotAt(event.x(), event.y());
        NavigatorLayout.Button button = buttonAt(slot);
        NavigatorActionPayload action = null;
        if (button != null) {
            action = button.action();
        } else {
            NavigatorView.Entry entry = entryAt(slot);
            if (entry != null) {
                action = new NavigatorActionPayload(NavigatorActionPayload.Action.ENTRY, 0, Optional.of(entry.id()), event.button() == 1);
            }
        }
        if (action != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            ApeiraClientNetwork.sendToServer(action);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

    private NavigatorView.@Nullable Entry entryAt(int slot) {
        int index = slot - NavigatorLayout.FIRST_ENTRY;
        return layout.showsList() && index >= 0 && index < view.entries().size() ? view.entries().get(index) : null;
    }

    private static boolean sameShape(int @Nullable [] a, int @Nullable [] b) {
        return a == null ? b == null : b != null && java.util.Arrays.equals(a, b);
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
