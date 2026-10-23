package com.apeira.main.client.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.decor.client.SkinAccent;
import com.apeira.main.home.HomeData;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.navigator.NavigatorView;
import com.apeira.main.navigator.OpenNavigatorPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The navigator, Origin Realms' {@code /navigator} menu with its Realms pages, drawn natively from the captured
 * textures instead of as a chest of invisible items. The server sends each page as a {@link NavigatorView}; this screen
 * draws its layers and buttons ({@link NavigatorLayout}, hit-tested and lit up by their drawn faces, {@link NavigatorBounds}),
 * the list's player heads with their tooltips and the page number, and sends every click as a
 * {@link NavigatorActionPayload}. It changes nothing itself: the server answers
 * each click with the next view, or closes the screen.
 */
public final class ApeiraNavigatorScreen extends Screen {
    /** The chest Origin Realms draws the navigator in; keeping its size keeps every layer and slot where it was. */
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 222;
    /** Where a 256x256, ascent-19 title glyph's top edge lands relative to the chest's top. */
    private static final int LAYER_Y = -6;
    /** The white of vanilla's slot highlight, at full strength. */
    private static final int HIGHLIGHT_ALPHA = 0x60;
    /** How long a button's highlight takes to fade in, in milliseconds. */
    private static final long HIGHLIGHT_FADE_MS = 120;
    /** The page number, "1/17": centred between the page arrows, in Origin Realms' page text colour. */
    private static final int PAGE_TEXT_X = 70;
    private static final int PAGE_TEXT_Y = 115;
    private static final int PAGE_TEXT_COLOR = 0xFFC6C6C6;
    /** The black frame drawn around every player head, half a GUI pixel wide, finer than the buttons' 1-pixel outline. */
    private static final int HEAD_FRAME = 0xFF000000;
    /** The profile's dark panel (inside its outline), where the player is drawn on their skin's accent colour. */
    private static final int PROFILE_X0 = 26;
    private static final int PROFILE_Y0 = 54;
    private static final int PROFILE_X1 = 94;
    private static final int PROFILE_Y1 = 124;
    /** The player model's size in the panel (pixels per block) and the name's baseline gap from the panel's bottom. */
    private static final int PROFILE_MODEL_SCALE = 30;
    private static final int PROFILE_NAME_GAP = 11;
    /** How the model stands: turned a little towards the buttons, as an inventory preview, without following the mouse. */
    private static final float PROFILE_BODY_YAW = 160.0F;
    /** The rename box, under the Homes popup. */
    private static final int RENAME_X = 32;
    private static final int RENAME_Y = 120;
    private static final int RENAME_WIDTH = 112;

    private NavigatorView view;
    private NavigatorLayout.Page layout;
    private int leftPos;
    private int topPos;
    /** Finds each button's face in the layers; each button's and each entry's faces, in frame coordinates. */
    private final NavigatorBounds bounds = new NavigatorBounds();
    private List<List<NavigatorBounds.Rect>> buttonFaces = List.of();
    private List<List<NavigatorBounds.Rect>> entryFaces = List.of();
    /** What the mouse is over: a button's index, {@link #entryHover} of an entry's, or {@link #NOTHING}. */
    private int hovered = NOTHING;
    private long hoverStart;
    private static final int NOTHING = Integer.MIN_VALUE;
    /** The box for a home's new name while one is being renamed, and that home's place in the grid. */
    private @Nullable EditBox rename;
    private int renameIndex;

    public ApeiraNavigatorScreen(NavigatorView view) {
        super(Component.translatable("screen.apeira.navigator"));
        this.view = view;
        this.layout = NavigatorLayout.of(view);
        measure();
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
        measure();
        if (view.page() != NavigatorPage.HOMES) {
            stopRenaming();
        }
    }

    /** Finds where the page's buttons and list cells are drawn. */
    private void measure() {
        List<List<NavigatorBounds.Rect>> buttons = new ArrayList<>();
        for (NavigatorLayout.Button button : layout.buttons()) {
            buttons.add(bounds.measure(layout.layers(), button.slots()));
        }
        List<List<NavigatorBounds.Rect>> entries = new ArrayList<>();
        for (int slot : layout.entrySlots()) {
            entries.add(bounds.measure(layout.layers(), new int[] {slot}));
        }
        buttonFaces = buttons;
        entryFaces = entries;
    }

    private void startRenaming(int index, String name) {
        stopRenaming();
        rename = new EditBox(font, leftPos + RENAME_X, topPos + RENAME_Y, RENAME_WIDTH, 16, Component.translatable("navigator.apeira.rename_home"));
        rename.setMaxLength(HomeData.MAX_NAME_LENGTH);
        rename.setValue(name);
        rename.setHint(Component.translatable("navigator.apeira.rename_home.hint"));
        renameIndex = index;
        // Added as a widget for input only; it is drawn after the layers, which would cover it.
        addWidget(rename);
        setFocused(rename);
    }

    private void stopRenaming() {
        if (rename != null) {
            removeWidget(rename);
            rename = null;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (rename != null) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                ApeiraClientNetwork.sendToServer(new NavigatorActionPayload(NavigatorActionPayload.Action.RENAME_HOME, renameIndex,
                        Optional.empty(), false, rename.getValue().trim()));
                stopRenaming();
                return true;
            }
            if (event.key() == InputConstants.KEY_ESCAPE) {
                stopRenaming();
                return true;
            }
            // Every other key goes to the box, so typing a name never triggers anything else.
            super.keyPressed(event);
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE && view.page() == NavigatorPage.PROFILE_OTHER) {
            // A profile is a popup over the friends list: Escape goes back to the list.
            ApeiraClientNetwork.sendToServer(NavigatorActionPayload.of(NavigatorActionPayload.Action.CLOSE_POPUP));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void init() {
        leftPos = (width - IMAGE_WIDTH) / 2;
        topPos = (height - IMAGE_HEIGHT) / 2;
        rename = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        for (Identifier layer : layout.layers()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, layer, leftPos, topPos + LAYER_Y, 0.0F, 0.0F, 256, 256, 256, 256);
        }
        int[] entrySlots = layout.entrySlots();
        for (int i = 0; i < Math.min(view.entries().size(), entrySlots.length); i++) {
            head(graphics, view.entries().get(i).id(), slotX(entrySlots[i]), slotY(entrySlots[i]), 16);
        }
        if (layout.pageNumbers()) {
            String pageText = (view.pageIndex() + 1) + "/" + view.pageCount();
            graphics.text(font, pageText, leftPos + PAGE_TEXT_X - font.width(pageText) / 2, topPos + PAGE_TEXT_Y, PAGE_TEXT_COLOR, false);
        }
        if (rename != null) {
            rename.extractRenderState(graphics, mouseX, mouseY, a);
        }
        view.target().ifPresent(target -> {
            if (view.page() == NavigatorPage.PROFILE_OTHER) {
                profile(graphics, target);
            } else {
                head(graphics, target.id(), slotX(NavigatorLayout.POPUP_HEAD), slotY(NavigatorLayout.POPUP_HEAD), 16);
            }
        });

        int hover = hoverAt(mouseX, mouseY);
        if (hover != hovered) {
            hovered = hover;
            hoverStart = Util.getMillis();
        }
        if (hover != NOTHING) {
            // The button's drawn face lights up, fading in, instead of the chest slots under it: the face is found in
            // the layers, so the highlight fits the button however it sits on the slot grid.
            float fade = Math.min(1.0F, (Util.getMillis() - hoverStart) / (float) HIGHLIGHT_FADE_MS);
            int color = ARGB.color(Math.round(HIGHLIGHT_ALPHA * fade), 0xFFFFFF);
            for (NavigatorBounds.Rect face : faces(hover)) {
                graphics.fill(leftPos + face.x0(), topPos + face.y0(), leftPos + face.x1(), topPos + face.y1(), color);
            }
            List<Component> tooltip = hover >= 0 ? layout.buttons().get(hover).tooltip() : entryTooltip(view.entries().get(entryIndex(hover)));
            graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    /**
     * Another player's profile: their live player model, standing still with its arms a little out, on their skin's
     * accent colour ({@link SkinAccent}) in the dark panel, with their name under it.
     */
    private void profile(GuiGraphicsExtractor graphics, NavigatorView.Entry target) {
        int x0 = leftPos + PROFILE_X0;
        int y0 = topPos + PROFILE_Y0;
        int x1 = leftPos + PROFILE_X1;
        int y1 = topPos + PROFILE_Y1;
        PlayerSkin skin = minecraft.playerSkinRenderCache().getOrDefault(ResolvableProfile.createUnresolved(target.id())).playerSkin();
        int accent = SkinAccent.of(skin.body().texturePath());
        graphics.fillGradient(x0, y0, x1, y1, ARGB.scaleRGB(accent, 1.1F), ARGB.scaleRGB(accent, 0.55F));

        AvatarRenderState state = new AvatarRenderState();
        state.entityType = EntityType.PLAYER;
        state.skin = skin;
        state.boundingBoxWidth = 0.6F;
        state.boundingBoxHeight = 1.8F;
        state.bodyRot = PROFILE_BODY_YAW;
        state.yRot = 0.0F;
        state.xRot = 0.0F;
        // A fixed age keeps the idle arm sway still, at its widest; ITEM holds both arms a little forward.
        state.ageInTicks = 0.0F;
        state.rightArmPose = HumanoidModel.ArmPose.ITEM;
        state.leftArmPose = HumanoidModel.ArmPose.ITEM;
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
        graphics.entity(state, PROFILE_MODEL_SCALE, translation, rotation, null, x0, y0, x1, y1 - PROFILE_NAME_GAP);

        graphics.text(font, target.name(), (x0 + x1) / 2 - font.width(target.name()) / 2, y1 - PROFILE_NAME_GAP + 1, 0xFFFFFFFF, true);
    }

    /**
     * A player's face, {@code size} pixels square, in a black frame half a GUI pixel wide, as every head in the navigator
     * is drawn. The frame is filled at double resolution, so it is finer than the buttons' outlines.
     */
    private static void head(GuiGraphicsExtractor graphics, UUID id, int x, int y, int size) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        graphics.fill(2 * x - 1, 2 * y - 1, 2 * (x + size) + 1, 2 * (y + size) + 1, HEAD_FRAME);
        graphics.pose().popMatrix();
        PlayerFaceExtractor.extractRenderState(graphics, ResolvableProfile.createUnresolved(id), x, y, size);
    }

    private static List<Component> entryTooltip(NavigatorView.Entry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.name()).withColor(NavigatorService.LABEL));
        lines.addAll(entry.lines());
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (rename != null) {
            if (rename.isMouseOver(event.x(), event.y())) {
                return super.mouseClicked(event, doubleClick);
            }
            stopRenaming();
        }
        int hover = hoverAt(event.x(), event.y());
        NavigatorActionPayload action = null;
        boolean secondary = event.button() == 1;
        if (hover >= 0) {
            NavigatorLayout.Button button = layout.buttons().get(hover);
            action = button.action() == null ? null : button.action().withSecondary(secondary);
        } else if (hover != NOTHING) {
            int index = entryIndex(hover);
            NavigatorView.Entry entry = view.entries().get(index);
            boolean shift = minecraft.hasShiftDown();
            if (shift && !secondary && view.page() == NavigatorPage.HOMES) {
                startRenaming(index, entry.name());
                return true;
            }
            action = new NavigatorActionPayload(NavigatorActionPayload.Action.ENTRY, index << 1 | (shift ? 1 : 0), Optional.of(entry.id()),
                    secondary, "");
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

    /**
     * What is under the mouse: a button's index (buttons first, in order, as popups sit over the page), an entry's
     * {@link #entryHover}, or {@link #NOTHING}.
     */
    private int hoverAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        for (int i = 0; i < buttonFaces.size(); i++) {
            for (NavigatorBounds.Rect face : buttonFaces.get(i)) {
                if (face.contains(x, y)) {
                    return i;
                }
            }
        }
        for (int i = 0; i < Math.min(entryFaces.size(), view.entries().size()); i++) {
            for (NavigatorBounds.Rect face : entryFaces.get(i)) {
                if (face.contains(x, y)) {
                    return entryHover(i);
                }
            }
        }
        return NOTHING;
    }

    private List<NavigatorBounds.Rect> faces(int hover) {
        return hover >= 0 ? buttonFaces.get(hover) : entryFaces.get(entryIndex(hover));
    }

    /** The hover value of entry {@code index}: negative, so it never clashes with a button's index. */
    private static int entryHover(int index) {
        return -1 - index;
    }

    private static int entryIndex(int hover) {
        return -1 - hover;
    }

    private int slotX(int slot) {
        return leftPos + NavigatorBounds.slotX(slot);
    }

    private int slotY(int slot) {
        return topPos + NavigatorBounds.slotY(slot);
    }
}
