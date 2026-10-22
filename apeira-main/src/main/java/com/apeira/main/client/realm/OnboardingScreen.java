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
 * Home..." until the player arrives in their realm.
 * <p>
 * Positions are in GUI pixels, measured from Origin Realms at GUI scale 3, where every card texture is drawn 1:1.
 */
public final class OnboardingScreen extends Screen {
    private static final int HOLD_TICKS = 60;
    /** A press of F released before this many ticks is a tap, which cycles the region. */
    private static final int TAP_TICKS = 5;
    private static final int FADE_TICKS = 8;
    private static final int BLACKOUT_TICKS = 10;
    private static final int CREATED_TICKS = 30;
    /** Closes the screen if the player has not arrived by then, so it cannot hang over the game. */
    private static final int TRAVEL_TIMEOUT = 400;

    private static final int REGION_WIDTH = 2276;
    private static final int REGION_HEIGHT = 1224;
    private static final int BAR_WIDTH = 136;
    private static final int ICON_SIZE = 24;
    private static final int ICON_SPACING = 28;
    private static final int PROGRESS_COLOR = 0xFF3A3A3A;
    private static final int DARK_COLOR = 0xFF101216;

    private static final Identifier GUIDE = texture("guide");
    private static final Identifier PRESETS = texture("presets");
    private static final Identifier ACTIVE = texture("active");
    private static final Identifier CONFIRM_BG = texture("confirm_big_bg");
    private static final Identifier CONFIRM_TEXT = texture("confirm_big_text");
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
            onClose();
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
        if (progress > 0.0F) {
            graphics.fill(barX, barY, barX + Math.round(BAR_WIDTH * progress), barY + 20, PROGRESS_COLOR);
        }
        blit(graphics, CONFIRM_TEXT, barX, barY, BAR_WIDTH, 20, -1);
        if (stage == Stage.BLACKOUT) {
            graphics.fill(0, 0, width, height, ARGB.color(Mth.clamp((stageTicks + a) / BLACKOUT_TICKS, 0.0F, 1.0F), 0));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return stage == Stage.INTRO || stage == Stage.PREVIEW;
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
