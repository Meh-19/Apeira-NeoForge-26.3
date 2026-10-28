package com.apeira.main.client.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.client.OutlinedEntityRenderer;
import com.apeira.decor.client.SkinAccent;
import com.apeira.decor.client.WornCosmetic;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.decor.network.OpenWardrobePayload;
import com.apeira.main.home.HomeData;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorView;
import com.apeira.main.navigator.OpenNavigatorPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
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
    /** The black frame drawn around every player head: a 16-pixel box whose 1-pixel border holds a 14-pixel face. */
    private static final int HEAD_FRAME = 0xFF000000;
    private static final int HEAD_SIZE = 16;
    /** An invite's head on the party roster is drawn faded. */
    private static final int INVITE_TINT = 0x80FFFFFF;
    /** The leader's pick on the party roster: a white frame around the head. */
    private static final int PICK_FRAME = 0xFFFFFFFF;
    /** The cosmetic cells' fill: vanilla's slot grey, which the server's chest shows through the texture's holes. */
    private static final int COSMETIC_CELL = 0xFF8B8B8B;
    private static final Identifier[] COSMETIC_ICONS = {icon("slot_hat"), icon("slot_back"), icon("slot_hand"), icon("slot_balloon")};
    /**
     * The status icons (Origin Realms' online, AFK and offline lights: green, blue and red, 9x9 with a black outline),
     * drawn in the model panel's corner where the server pack's 16-pixel glyph puts them, 3 pixels in from the status
     * cell's left and 5 down.
     */
    private static final Identifier STATUS_ONLINE = icon("status_online");
    private static final Identifier STATUS_AFK = icon("status_afk");
    private static final Identifier STATUS_OFFLINE = icon("status_offline");
    private static final int STATUS_SIZE = 9;
    private static final int STATUS_X = 3;
    private static final int STATUS_Y = 5;
    /**
     * Another player's profile title, "Name's Profile" and a line to a fixed right edge, in the server pack's
     * {@code menus/profile/name} font ({@code apeira:profile_name}: the ASCII sheet at ascent -11, so the text lands 18
     * pixels under where it is drawn). Drawn where the captured title is, (7, 6) moved one row down into this 6-row
     * frame, in the dark grey of the own profile's baked-in "Your Profile". The line is filled in that grey too, where
     * the own profile's line is (one pixel tall, {@link #TITLE_LINE_Y}); the server's {@code line.png} glyphs drew it
     * white.
     */
    private static final FontDescription PROFILE_NAME_FONT = new FontDescription.Resource(ApeiraIds.id("profile_name"));
    private static final int TITLE_X = 7;
    private static final int TITLE_Y = 24;
    private static final int TITLE_COLOR = 0xFF555555;
    /** The space between the name and the line, and where the line ends: name, gap and line are this wide. */
    private static final int TITLE_GAP = 6;
    private static final int TITLE_WIDTH = 162;
    /** The line's row in the frame: the text's top (TITLE_Y + 18) plus 4, as in the own profile's layer. */
    private static final int TITLE_LINE_Y = TITLE_Y + 22;
    /** The profile's dark panel (inside its outline), where the player is drawn on their skin's accent colour. */
    private static final int PROFILE_X0 = 26;
    private static final int PROFILE_Y0 = 54;
    private static final int PROFILE_X1 = 94;
    private static final int PROFILE_Y1 = 124;
    /**
     * The player model's view, a three-quarter turn as on Origin Realms' profile: turned 45 degrees about its up axis,
     * then tipped 34.5 degrees towards the viewer, so the top of the head and one side show.
     */
    private static final float VIEW_PITCH = 34.5F;
    private static final float VIEW_YAW = 45.0F;
    /** The avatar renderer's own scale: a drawn player is 1.875 blocks tall, not 2. */
    private static final float AVATAR_SCALE = 0.9375F;
    /**
     * The posed player's outline in blocks (x across, y up from the feet, z front to back), before the avatar scale:
     * head, body with both arms (a little out, as in the idle pose), legs. The model is fitted to the panel by these boxes.
     */
    private static final float[][] MODEL_BOXES = {
            {-0.25F, 1.5F, -0.25F, 0.25F, 2.0F, 0.25F},
            {-0.58F, 0.7F, -0.13F, 0.58F, 1.5F, 0.13F},
            {-0.25F, 0.0F, -0.13F, 0.25F, 0.75F, 0.13F}};
    /** How many boxes of {@link #MODEL_BOXES}, from the first, make the bust: head, and body with arms. */
    private static final int BUST_BOXES = 2;
    /** The space kept between the model's outline and the panel's edge, in GUI pixels; the toon outline takes one. */
    private static final int MODEL_MARGIN = 4;
    /**
     * The bust shot: the model is drawn this many times the size that fits it whole in the panel, so the legs and lower
     * body fall below the panel and only the head, arms and upper torso show.
     */
    private static final float BUST_ZOOM = 2.0F;
    /** How far below the panel's top edge the top of the head sits, in GUI pixels. */
    private static final int BUST_TOP = 8;
    /**
     * The icons drawn inside the dark panel, which the accent gradient would cover (frame coordinates, measured in the
     * layers): the Profile Info icon in the top right corner of both profiles, and the own profile's featured badge.
     * Each is drawn again over the gradient and the model from its own texture, cut from the layer with the panel's
     * grey left transparent, so the gradient runs on behind the badge's rounded corners instead of a grey square.
     */
    private static final NavigatorBounds.Rect INFO_ICON = new NavigatorBounds.Rect(82, 56, 92, 66);
    private static final NavigatorBounds.Rect NPC_GLOBE = new NavigatorBounds.Rect(48, 75, 58, 85);
    private static final NavigatorBounds.Rect FEATURED_BADGE = new NavigatorBounds.Rect(26, 54, 42, 70);
    private static final Identifier INFO_ICON_TEXTURE = icon("info_icon");
    private static final Identifier FEATURED_BADGE_TEXTURE = icon("featured_badge");
    /**
     * The player inventory part of vanilla's 6-row chest, {@code generic_54}: only it backs the navigator, so where the
     * layers stop the player's inventory grid shows and nothing of the chest's rows.
     */
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int CHEST_ROWS_HEIGHT = 6 * 18 + 17;
    /**
     * The player inventory under the chest, its three rows and the hotbar, as vanilla's container lays them. Its
     * "Inventory" label is left out: it would be drawn over the layers, which reach down past it.
     */
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = IMAGE_HEIGHT - 82;
    private static final int HOTBAR_Y = IMAGE_HEIGHT - 24;
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
        this.layout = NavigatorLayout.of(view, self());
        measure();
    }

    private static Identifier icon(String name) {
        return ApeiraIds.id("textures/gui/navigator/profile/" + name + ".png");
    }

    /** The viewing player, left out of Social's party row. */
    private static UUID self() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? Util.NIL_UUID : player.getUUID();
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
        this.layout = NavigatorLayout.of(view, self());
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
        if (layout.profile()) {
            // The status, the info icon and the featured badge sit on the plain dark panel, so a fill finds no outline
            // around them: their highlight is the drawn indicator's own box instead of the slot cell.
            for (int i = 0; i < layout.buttons().size(); i++) {
                int[] slots = layout.buttons().get(i).slots();
                if (slots.length == 1 && slots[0] == NavigatorLayout.STATUS_SLOT) {
                    buttons.set(i, List.of(statusFace()));
                } else if (slots.length == 1 && slots[0] == NavigatorLayout.INFO_SLOT) {
                    buttons.set(i, List.of(INFO_ICON));
                } else if (slots.length == 1 && slots[0] == NavigatorLayout.BADGE_SLOTS[0] && view.page() == NavigatorPage.PROFILE) {
                    // The featured badge is drawn over the gradient from its own texture, so its box is the highlight.
                    buttons.set(i, List.of(FEATURED_BADGE));
                }
            }
        }
        if (view.page() == NavigatorPage.EDIT_NPC) {
            // The globe's black lines would cut its fill short: its highlight is the icon's outlined box.
            for (int i = 0; i < layout.buttons().size(); i++) {
                int[] slots = layout.buttons().get(i).slots();
                if (slots.length == 1 && slots[0] == NavigatorLayout.NPC_GLOBE_SLOT) {
                    buttons.set(i, List.of(NPC_GLOBE));
                }
            }
        }
        buttonFaces = buttons;
        entryFaces = entries;
    }

    /**
     * The status icon's box in frame coordinates, outline included. Its hover highlight and hitbox are exactly this box.
     */
    private static NavigatorBounds.Rect statusFace() {
        int x0 = NavigatorBounds.slotX(NavigatorLayout.STATUS_SLOT) + STATUS_X;
        int y0 = NavigatorBounds.slotY(NavigatorLayout.STATUS_SLOT) + STATUS_Y;
        return new NavigatorBounds.Rect(x0, y0, x0 + STATUS_SIZE, y0 + STATUS_SIZE);
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
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(event))) {
            // The inventory key closes the navigator from any page, as it closes the server's chest menu.
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE && view.page() == NavigatorPage.PROFILE_OTHER) {
            // A profile is a popup over the friends list or the party roster: Escape goes back to it, or closes the
            // navigator when the profile was opened from the world.
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
        inventory(graphics, mouseX, mouseY);
        for (Identifier layer : layout.layers()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, layer, leftPos, topPos + LAYER_Y, 0.0F, 0.0F, 256, 256, 256, 256);
        }
        int[] entrySlots = layout.entrySlots();
        for (int i = 0; i < Math.min(view.entries().size(), entrySlots.length); i++) {
            head(graphics, view.entries().get(i).skin(), slotX(entrySlots[i]), slotY(entrySlots[i]), -1);
        }
        for (NavigatorLayout.Head head : layout.heads()) {
            int x = slotX(head.slot());
            int y = slotY(head.slot()) + head.dy();
            if (head.slot() == layout.picked()) {
                graphics.fill(x - 1, y - 1, x + HEAD_SIZE + 1, y + HEAD_SIZE + 1, PICK_FRAME);
            }
            head(graphics, head.skin(), x, y, head.dim() ? INVITE_TINT : -1);
        }
        if (layout.pageNumbers()) {
            String pageText = (view.pageIndex() + 1) + "/" + view.pageCount();
            graphics.text(font, pageText, leftPos + PAGE_TEXT_X - font.width(pageText) / 2, topPos + PAGE_TEXT_Y, PAGE_TEXT_COLOR, false);
        }
        if (rename != null) {
            rename.extractRenderState(graphics, mouseX, mouseY, a);
        }
        view.target().ifPresent(target -> {
            if (layout.profile()) {
                profile(graphics, target);
            } else if (view.page() == NavigatorPage.EDIT_PLAYER || view.page() == NavigatorPage.EDIT_MEMBER) {
                head(graphics, target.skin(), slotX(NavigatorLayout.POPUP_HEAD), slotY(NavigatorLayout.POPUP_HEAD), -1);
            }
        });

        int hover = hoverAt(mouseX, mouseY);
        if (hover != hovered) {
            hovered = hover;
            hoverStart = Util.getMillis();
        }
        if (hover != NOTHING) {
            // The button's drawn face lights up, fading in, instead of the chest slots under it: the face is found in
            // the layers, so the highlight fits the button however it sits on the slot grid. Unlit buttons (the
            // profile's model panel) only show their tooltip.
            if (hover < 0 || layout.buttons().get(hover).lit()) {
                float fade = Math.min(1.0F, (Util.getMillis() - hoverStart) / (float) HIGHLIGHT_FADE_MS);
                int color = ARGB.color(Math.round(HIGHLIGHT_ALPHA * fade), 0xFFFFFF);
                for (NavigatorBounds.Rect face : faces(hover)) {
                    graphics.fill(leftPos + face.x0(), topPos + face.y0(), leftPos + face.x1(), topPos + face.y1(), color);
                }
            }
            List<Component> tooltip = hover >= 0 ? layout.buttons().get(hover).tooltip()
                    : NavigatorLayout.entryTooltip(view.entries().get(entryIndex(hover)));
            graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    /**
     * The player's inventory grid under the layers, with the hovered item's tooltip: the server's menu is a chest, so its
     * player inventory shows where the layers stop. Only the inventory part of the chest texture is drawn, not its rows.
     * The items are only shown; the navigator takes no clicks on them.
     */
    private void inventory(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + CHEST_ROWS_HEIGHT, 0.0F, 126.0F, IMAGE_WIDTH, 96, 256, 256);
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < 36; slot++) {
            // Slots 0-8 are the hotbar, along the bottom; 9-35 the three rows above it.
            int x = leftPos + INVENTORY_X + slot % 9 * 18;
            int y = topPos + (slot < 9 ? HOTBAR_Y : INVENTORY_Y + (slot / 9 - 1) * 18);
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    /**
     * A profile, the player's own or another's: their player model ({@link #model}), standing still with its arms a little
     * out and drawn with a black toon outline ({@link OutlinedEntityRenderer}), on their skin's accent colour
     * ({@link SkinAccent}: their eyes' or face's most distinct colour, fading to near black at the bottom) in the dark panel, with no name under it, as on the server; the status icon (online, AFK or offline) in the panel's corner; the
     * cosmetic slots down the left, each showing what is worn as its 3D model or, when empty, the slot's icon; and the
     * "Name's Profile" title. The model wears no cosmetics: they are shown in the sidebar.
     */
    private void profile(GuiGraphicsExtractor graphics, NavigatorView.Entry target) {
        for (int i = 0; i < NavigatorLayout.COSMETIC_SLOTS.length; i++) {
            int x = slotX(NavigatorLayout.COSMETIC_SLOTS[i]);
            int y = slotY(NavigatorLayout.COSMETIC_SLOTS[i]);
            graphics.fill(x, y, x + 16, y + 16, COSMETIC_CELL);
            CosmeticSlot[] worn = CosmeticSlot.values();
            ItemStack stack = i < worn.length ? view.cosmetics().get(worn[i]) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, COSMETIC_ICONS[i], x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            } else {
                // Resolved as worn, so the item definition picks the cosmetic's 3D model (with its gui transform)
                // rather than the rarity token it shows in inventories.
                WornCosmetic.whileWorn(() -> graphics.item(stack, x, y));
            }
        }
        // The own profile's layer has "Your Profile" and its line baked in; only another's is written here.
        if (view.page() == NavigatorPage.PROFILE_OTHER) {
            title(graphics, target.name());
        }

        int x0 = leftPos + PROFILE_X0;
        int y0 = topPos + PROFILE_Y0;
        int x1 = leftPos + PROFILE_X1;
        int y1 = topPos + PROFILE_Y1;
        PlayerSkin skin = minecraft.playerSkinRenderCache().getOrDefault(target.skin()).playerSkin();
        int accent = SkinAccent.of(skin.body().texturePath());
        // The accent at full strength at the top, half way down at half, then on to near black at the bottom edge.
        int middle = y0 + (y1 - y0) * 11 / 20;
        int half = ARGB.scaleRGB(accent, 0.5F);
        graphics.fillGradient(x0, y0, x1, middle, ARGB.scaleRGB(accent, 1.1F), half);
        graphics.fillGradient(x0, middle, x1, y1, half, ARGB.scaleRGB(accent, 0.08F));

        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        state.boundingBoxWidth = 0.6F;
        state.boundingBoxHeight = 1.8F;
        // Facing south, towards the viewer before the view's turn. A new state has no name tag, so no name is drawn.
        state.bodyRot = 0.0F;
        state.yRot = 0.0F;
        state.xRot = 0.0F;
        // Empty hands at age 0 hold the idle arm sway still with both arms 0.1 radians out: the idle pose's slight spread.
        state.ageInTicks = 0.0F;
        state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model(graphics, state);
        // The gradient covers the layers' icons in the panel; they are drawn again over it and the model.
        overlay(graphics, INFO_ICON_TEXTURE, INFO_ICON);
        if (view.page() == NavigatorPage.PROFILE) {
            overlay(graphics, FEATURED_BADGE_TEXTURE, FEATURED_BADGE);
        }

        // The status is drawn in exactly its face (statusFace), so its highlight matches the icon.
        Identifier status = view.has(NavigatorView.TARGET_AFK) ? STATUS_AFK : view.has(NavigatorView.TARGET_ONLINE) ? STATUS_ONLINE : STATUS_OFFLINE;
        overlay(graphics, status, statusFace());
    }

    /**
     * Another player's profile title as the server writes it: "Name's Profile" ("Name' Profile" after an s), a 6-pixel
     * gap, then a line in the same grey to {@link #TITLE_WIDTH} pixels from the title's start.
     */
    private void title(GuiGraphicsExtractor graphics, String name) {
        String text = name + (name.endsWith("s") ? "'" : "'s") + " Profile";
        Component title = Component.literal(text).withStyle(Style.EMPTY.withFont(PROFILE_NAME_FONT));
        graphics.text(font, title, leftPos + TITLE_X, topPos + TITLE_Y, TITLE_COLOR, false);
        int lineX = leftPos + TITLE_X + font.width(title) + TITLE_GAP;
        int lineEnd = leftPos + TITLE_X + TITLE_WIDTH;
        if (lineX < lineEnd) {
            graphics.fill(lineX, topPos + TITLE_LINE_Y, lineEnd, topPos + TITLE_LINE_Y + 1, TITLE_COLOR);
        }
    }

    /** Draws {@code texture}, the size of {@code rect}, at {@code rect} (frame coordinates). */
    private void overlay(GuiGraphicsExtractor graphics, Identifier texture, NavigatorBounds.Rect rect) {
        int w = rect.x1() - rect.x0();
        int h = rect.y1() - rect.y0();
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + rect.x0(), topPos + rect.y0(), 0.0F, 0.0F, w, h, w, h);
    }

    /**
     * Draws {@code state} in the profile's panel as a bust, as the inventory screen draws its player: through the entity
     * renderer, in a picture-in-picture box the size of the panel, which cuts off what reaches past it. The pose is
     * worked out here, not read from a model file: the posed outline ({@link #MODEL_BOXES}) is turned by the view and its
     * on-screen bounds are measured; the model is scaled {@link #BUST_ZOOM} times the size that fits the whole outline in
     * the panel less {@link #MODEL_MARGIN}, centred across on the bust (head, body and arms), with the top of the head
     * {@link #BUST_TOP} below the panel's top, so the lower body falls out of the panel. The box's space has its origin
     * at the panel's centre, x right, y down, one unit per GUI pixel times the scale.
     */
    private void model(GuiGraphicsExtractor graphics, AvatarRenderState state) {
        // Flipped about x into the box's y-down space, then the view's tip and turn.
        Quaternionf rotation = new Quaternionf().rotationX((float) Math.PI)
                .mul(new Quaternionf().rotationXYZ(VIEW_PITCH * Mth.DEG_TO_RAD, VIEW_YAW * Mth.DEG_TO_RAD, 0.0F));
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        Vector3f bustMin = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f bustMax = new Vector3f(Float.NEGATIVE_INFINITY);
        Vector3f corner = new Vector3f();
        for (int b = 0; b < MODEL_BOXES.length; b++) {
            float[] box = MODEL_BOXES[b];
            for (int i = 0; i < 8; i++) {
                corner.set(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4], box[(i & 4) == 0 ? 2 : 5]).mul(AVATAR_SCALE);
                rotation.transform(corner);
                min.min(corner);
                max.max(corner);
                if (b < BUST_BOXES) {
                    bustMin.min(corner);
                    bustMax.max(corner);
                }
            }
        }
        int x0 = leftPos + PROFILE_X0;
        int y0 = topPos + PROFILE_Y0;
        int x1 = leftPos + PROFILE_X1;
        int y1 = topPos + PROFILE_Y1;
        float pixels = BUST_ZOOM * Math.min((x1 - x0 - 2 * MODEL_MARGIN) / (max.x - min.x), (y1 - y0 - 2 * MODEL_MARGIN) / (max.y - min.y));
        // In blocks, turned: the bust's middle across goes to the box's centre, the top of the head BUST_TOP pixels under
        // the box's top edge, and the whole outline's middle in depth to the box's, so no part is cut by the near or far
        // plane.
        Vector3f translation = new Vector3f(-(bustMin.x + bustMax.x) * 0.5F, (BUST_TOP - (y1 - y0) * 0.5F) / pixels - bustMin.y,
                -(min.z + max.z) * 0.5F);
        OutlinedEntityRenderer.submit(graphics, state, pixels, translation, rotation, x0, y0, x1, y1);
    }

    /**
     * A player's head in the 16-pixel cell at {@code (x, y)}, as every head in the navigator is drawn: a black border one
     * pixel wide around a 14-pixel face, tinted by {@code color}.
     */
    private void head(GuiGraphicsExtractor graphics, ResolvableProfile profile, int x, int y, int color) {
        graphics.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, HEAD_FRAME);
        PlayerSkin skin = minecraft.playerSkinRenderCache().getOrDefault(profile).playerSkin();
        PlayerFaceExtractor.extractRenderState(graphics, skin, x + 1, y + 1, HEAD_SIZE - 2, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (rename != null) {
            if (rename.isMouseOver(event.x(), event.y())) {
                return super.mouseClicked(event, doubleClick);
            }
            stopRenaming();
        }
        // The physical left and right buttons (SDL's ids in 26.x: left 1, right 3), not the attack and use bindings.
        int mouseButton = event.button();
        if (mouseButton != InputConstants.MOUSE_BUTTON_LEFT && mouseButton != InputConstants.MOUSE_BUTTON_RIGHT) {
            return super.mouseClicked(event, doubleClick);
        }
        int hover = hoverAt(event.x(), event.y());
        NavigatorActionPayload action = null;
        boolean secondary = mouseButton == InputConstants.MOUSE_BUTTON_RIGHT;
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
            if (action.action() == NavigatorActionPayload.Action.WARDROBE) {
                // The wardrobe is decor's menu; the navigator closes as it opens.
                ApeiraClientNetwork.sendToServer(OpenWardrobePayload.INSTANCE);
            } else {
                ApeiraClientNetwork.sendToServer(action);
            }
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
