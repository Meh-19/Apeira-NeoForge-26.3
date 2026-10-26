package com.apeira.main.client.realm;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.realm.OpenOnboardingPayload;
import com.apeira.main.realm.RealmCreatedPayload;
import com.apeira.main.realm.RealmPreset;
import com.apeira.main.realm.SelectRealmPresetPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Origin Realms' realm creation screen, drawn from its textures ({@code textures/gui/onboarding/}). The guide card
 * comes first ("Press F to continue"); then each region is shown full screen with its card at the top and the five
 * region icons and the "Hold F to pick" bar at the bottom. Keys 1 to 5 pick a region, tapping F goes to the next one
 * (tropical wraps to lush), and holding F for {@link #HOLD_TICKS} fills the bar, fades to black and sends a
 * {@link SelectRealmPresetPayload}. The server's {@link RealmCreatedPayload} shows "Realm Created", then "Traveling
 * Home..." until the player arrives in their realm. The screen fades in from black, and it cannot be closed (Esc or
 * otherwise) until a region is picked.
 * <p>
 * Positions are in GUI pixels, measured from Origin Realms at GUI scale 3, where every card texture is drawn 1:1.
 */
public final class OnboardingScreen extends Screen {
    private static final int HOLD_TICKS = 40;
    /** A press of F released before this many ticks is a tap, which cycles the region. */
    private static final int TAP_TICKS = 5;
    private static final int FADE_TICKS = 8;
    private static final int FADE_IN_TICKS = 20;
    private static final int BLACKOUT_TICKS = 10;
    private static final int CREATED_TICKS = 30;
    /** Closes the screen if the player has not arrived by then, so it cannot hang over the game. */
    private static final int TRAVEL_TIMEOUT = 400;

    private static final int REGION_WIDTH = 2276;
    private static final int REGION_HEIGHT = 1224;
    private static final int BAR_WIDTH = 136;
    private static final int ICON_SIZE = 24;
    private static final int ICON_SPACING = 28;
    /**
     * {@code confirm_progress.png} is Origin Realms' progress font: seven 20-pixel rows of the bar's fill, 64, 32, 16,
     * 8, 4, 2 and 1 pixels wide, which it adds up to any width.
     */
    private static final int PROGRESS_ROW_HEIGHT = 20;
    private static final int PROGRESS_TEXTURE_WIDTH = 64;
    private static final int PROGRESS_TEXTURE_HEIGHT = 140;
    private static final int DARK_COLOR = 0xFF101216;

    private static final Identifier GUIDE = texture("guide");
    private static final Identifier PRESETS = texture("presets");
    private static final Identifier ACTIVE = texture("active");
    private static final Identifier CONFIRM_BG = texture("confirm_big_bg");
    private static final Identifier CONFIRM_TEXT = texture("confirm_big_text");
    private static final Identifier CONFIRM_PROGRESS = texture("confirm_progress");
    private static final Identifier CREATED = texture("created");
    private static final Identifier TRAVELING = texture("traveling");

    private Stage stage = Stage.INTRO;
    private int stageTicks;
    private RealmPreset preset = RealmPreset.LUSH;
    private @Nullable RealmPreset previous;
    private int fadeTicks;
    private boolean holdingF;
    /** Whether the current press of F was used up (continuing from the guide) and must not cycle or pick. */
    private boolean pressConsumed;
    private int holdTicks;
    private int openTicks;
    private @Nullable ResourceKey<Level> departure;

    public OnboardingScreen() {
        super(Component.translatable("screen.apeira.onboarding"));
    }

    public static void onOpen(OpenOnboardingPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof OnboardingScreen)) {
            minecraft.gui.setScreen(new OnboardingScreen());
        }
    }

    public static void onCreated(RealmCreatedPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().gui.screen() instanceof OnboardingScreen screen) {
            screen.setStage(Stage.CREATED);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_F) {
            if (!holdingF) {
                holdingF = true;
                holdTicks = 0;
                pressConsumed = stage != Stage.PREVIEW;
                if (stage == Stage.INTRO) {
                    showPreview(preset);
                }
            }
            return true;
        }
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_5 && (stage == Stage.INTRO || stage == Stage.PREVIEW)) {
            showPreview(RealmPreset.values()[key - InputConstants.KEY_1]);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_F) {
            releaseF();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public void tick() {
        stageTicks++;
        openTicks++;
        // The region pictures are large (2276x1224): load one a tick while the screen fades in from black, instead of
        // each on the frame it is first shown, which stalled the cross-fade.
        RealmPreset[] regions = RealmPreset.values();
        if (openTicks <= regions.length) {
            minecraft.getTextureManager().getTexture(texture(regions[openTicks - 1].getSerializedName() + "_region"));
        }
        if (fadeTicks > 0) {
            fadeTicks--;
        }
        if (holdingF && !InputConstants.isKeyDown(InputConstants.KEY_F)) {
            releaseF();
        }
        switch (stage) {
            case PREVIEW -> {
                if (holdingF && !pressConsumed && ++holdTicks >= HOLD_TICKS) {
                    holdingF = false;
                    setStage(Stage.BLACKOUT);
                }
            }
            case BLACKOUT -> {
                if (stageTicks >= BLACKOUT_TICKS) {
                    departure = minecraft.level != null ? minecraft.level.dimension() : null;
                    ApeiraClientNetwork.sendToServer(new SelectRealmPresetPayload(preset));
                    setStage(Stage.WAITING);
                }
            }
            case CREATED -> {
                if (stageTicks >= CREATED_TICKS) {
                    setStage(Stage.TRAVELING);
                }
            }
            default -> {}
        }
        boolean arrived = departure != null && minecraft.level != null && !minecraft.level.dimension().equals(departure);
        if (arrived || (stage == Stage.WAITING || stage == Stage.TRAVELING) && stageTicks > TRAVEL_TIMEOUT) {
            super.onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Each stage covers the whole screen itself.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (stage == Stage.WAITING || stage == Stage.CREATED || stage == Stage.TRAVELING) {
            graphics.fill(0, 0, width, height, DARK_COLOR);
            if (stage != Stage.WAITING) {
                Identifier card = stage == Stage.CREATED ? CREATED : TRAVELING;
                int cardWidth = stage == Stage.CREATED ? 104 : 122;
                blit(graphics, card, (width - cardWidth) / 2, height * 46 / 100 - 6, cardWidth, 12, -1);
            }
            return;
        }
        drawRegion(graphics, preset, -1);
        if (previous != null && fadeTicks > 0) {
            // The old region fades out over the new one.
            float alpha = Mth.clamp((fadeTicks - a) / FADE_TICKS, 0.0F, 1.0F);
            drawRegion(graphics, previous, ARGB.white(alpha));
        }
        int centreX = width / 2;
        if (stage == Stage.INTRO) {
            blit(graphics, GUIDE, centreX - 100, (height - 140) / 2, 200, 140, -1);
            fadeIn(graphics, a);
            return;
        }
        int barX = centreX - BAR_WIDTH / 2;
        blit(graphics, texture("select_" + preset.getSerializedName()), barX, 10, 135, 54, -1);
        int iconsY = height - 69;
        blit(graphics, PRESETS, barX, iconsY, BAR_WIDTH, ICON_SIZE, -1);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ACTIVE, barX + preset.ordinal() * ICON_SPACING, iconsY, preset.ordinal() * ICON_SIZE, 0,
                ICON_SIZE, ICON_SIZE, 120, ICON_SIZE);
        int barY = height - 41;
        blit(graphics, CONFIRM_BG, barX, barY, BAR_WIDTH, 20, -1);
        float progress = holdingF && !pressConsumed ? Mth.clamp((holdTicks + a) / HOLD_TICKS, 0.0F, 1.0F) : 0.0F;
        if (stage == Stage.BLACKOUT) {
            progress = 1.0F;
        }
        drawProgress(graphics, barX, barY, Math.round(BAR_WIDTH * progress));
        blit(graphics, CONFIRM_TEXT, barX, barY, BAR_WIDTH, 20, -1);
        if (stage == Stage.BLACKOUT) {
            graphics.fill(0, 0, width, height, ARGB.color(Mth.clamp((stageTicks + a) / BLACKOUT_TICKS, 0.0F, 1.0F), 0));
        } else {
            fadeIn(graphics, a);
        }
    }

    /** Covers the screen in black that clears over {@link #FADE_IN_TICKS} after it opens. */
    private void fadeIn(GuiGraphicsExtractor graphics, float a) {
        if (openTicks < FADE_IN_TICKS) {
            graphics.fill(0, 0, width, height, ARGB.color(Mth.clamp(1.0F - (openTicks + a) / FADE_IN_TICKS, 0.0F, 1.0F), 0));
        }
    }

    /** Fills {@code width} pixels of the bar from {@link #CONFIRM_PROGRESS}'s rows, as Origin Realms adds them up. */
    private static void drawProgress(GuiGraphicsExtractor graphics, int x, int y, int width) {
        while (width > 0) {
            int row = 0;
            int rowWidth = PROGRESS_TEXTURE_WIDTH;
            while (rowWidth > width) {
                rowWidth >>= 1;
                row++;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONFIRM_PROGRESS, x, y, 0.0F, row * PROGRESS_ROW_HEIGHT, rowWidth,
                    PROGRESS_ROW_HEIGHT, PROGRESS_TEXTURE_WIDTH, PROGRESS_TEXTURE_HEIGHT);
            x += rowWidth;
            width -= rowWidth;
        }
    }

    /** A realm must be picked first: Esc does nothing, and nothing else closes the screen before the pick. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if (stage != Stage.INTRO && stage != Stage.PREVIEW) {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void releaseF() {
        if (holdingF && !pressConsumed && stage == Stage.PREVIEW && holdTicks < TAP_TICKS) {
            showPreview(preset.next());
        }
        holdingF = false;
        holdTicks = 0;
    }

    private void showPreview(RealmPreset next) {
        if (stage == Stage.INTRO) {
            setStage(Stage.PREVIEW);
        }
        if (next != preset) {
            previous = preset;
            preset = next;
            fadeTicks = FADE_TICKS;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void setStage(Stage stage) {
        this.stage = stage;
        this.stageTicks = 0;
    }

    /** Draws a region's picture over the whole screen, cropped to keep its shape. */
    private void drawRegion(GuiGraphicsExtractor graphics, RealmPreset region, int color) {
        float scale = Math.max((float) width / REGION_WIDTH, (float) height / REGION_HEIGHT);
        int drawWidth = Mth.ceil(REGION_WIDTH * scale);
        int drawHeight = Mth.ceil(REGION_HEIGHT * scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture(region.getSerializedName() + "_region"), (width - drawWidth) / 2,
                (height - drawHeight) / 2, 0.0F, 0.0F, drawWidth, drawHeight, REGION_WIDTH, REGION_HEIGHT, REGION_WIDTH, REGION_HEIGHT, color);
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, width, height, width, height, color);
    }

    private static Identifier texture(String name) {
        return ApeiraIds.id("textures/gui/onboarding/" + name + ".png");
    }

    private enum Stage {
        /** The "Create Your Realm" guide card. */
        INTRO,
        PREVIEW,
        /** The hold finished: the screen fades to black before the pick is sent. */
        BLACKOUT,
        /** Sent; waiting for the server's answer. */
        WAITING,
        CREATED,
        TRAVELING
    }
}
