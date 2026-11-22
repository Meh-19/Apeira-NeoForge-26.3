package com.apeira.main.client.crate;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.Apeira;
import com.apeira.main.client.realm.RealmTopBar;
import com.apeira.main.crate.BadgerCrateItem;
import com.apeira.main.crate.BadgerCrateShowEntity;
import com.apeira.main.network.BadgerInputPayload;
import com.apeira.main.network.BadgerPhasePayload;
import com.apeira.main.registry.MainBadgerCrate;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The client side of a Badger Crate opening ({@code com.apeira.main.crate.BadgerSession}): the black fades, the
 * spectator camera locked to the recorded track ({@link BadgerCrateCamera}, applied by
 * {@code mixin/client/CameraMixin}), no HUD, inventory, movement or clicks, "Opening crate..." until ten seconds in,
 * then "Crouch to collect selected reward" with the scroll wheel picking between the three rewards. Also the hold
 * bar on the crate's slot ({@link #decorate}).
 */
public final class BadgerCrateClient {
    private static final Identifier LAYER = Apeira.REGISTRAR.id("badger_crate");
    private static final int FADE_TICKS = 10;
    /** The field of view in the crate world, as on the server. */
    private static final float FOV = 60.0F;
    private static final int READY_DELAY = 10;
    /** The vanilla cooldown overlay's white at half alpha. */
    private static final int HOLD_OVERLAY = 0x7FFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    /** The capture's text colours: Origin Realms' green and its grey. */
    private static final int GREEN = 0x85CC16;
    private static final int GREY = 0xA7AEBA;
    /** Where vanilla writes the first and second boss bars' names. */
    private static final int FIRST_BAR_Y = 3;
    private static final int SECOND_BAR_Y = 22;

    private static int phase = -1;
    private static int entityId = -1;
    /** The latest animation time the camera was placed at, so it never steps back (a frame drawn before the show entity's tick ran). */
    private static float cameraTicks;
    private static int phaseTicks;
    private static float fade;
    private static float fadeO;
    private static boolean readySent;
    private static boolean sneakWasDown;
    private static int selected;
    /** The animation tick the opening sounds were last played up to. */
    private static int soundTick = -1;

    private BadgerCrateClient() {}

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(BadgerCrateClient::registerLayer);
        modBus.addListener(BadgerCrateClient::registerDecorations);
        modBus.addListener(SelectionOutline::registerPipelines);
        gameBus.addListener(BadgerCrateClient::onClientTick);
        gameBus.addListener(BadgerCrateClient::onMovementInput);
        gameBus.addListener(BadgerCrateClient::onScroll);
        gameBus.addListener(BadgerCrateClient::onComputeFov);
        gameBus.addListener(BadgerCrateClient::onInteraction);
        gameBus.addListener(BadgerCrateClient::onRenderLayer);
        gameBus.addListener(BadgerCrateClient::onScreenOpening);
        gameBus.addListener(BadgerCrateClient::onScreenRender);
        gameBus.addListener(BadgerCrateClient::onLoggingOut);
    }

    public static void onPhase(BadgerPhasePayload payload, IPayloadContext context) {
        if (phase < 0 && payload.phase() != BadgerPhase.END) {
            BadgerCrateCamera.load();
            selected = 0;
        }
        phase = payload.phase();
        phaseTicks = 0;
        if (payload.entity() >= 0) {
            entityId = payload.entity();
            cameraTicks = 0.0F;
        }
        if (phase == BadgerPhase.LOADING) {
            readySent = false;
        }
        if (phase == BadgerPhase.SHOW) {
            soundTick = -1;
        }
        if (phase == BadgerPhase.END) {
            entityId = -1;
            cameraTicks = 0.0F;
            BadgerCrateFx.release();
        }
    }

    /** True from the fade out to the end of the fade back in. */
    public static boolean active() {
        return phase >= 0;
    }

    /** True while the camera follows the recorded track: from arriving in the instance to the last fade. */
    public static boolean cameraLocked() {
        return phase == BadgerPhase.LOADING || phase == BadgerPhase.SHOW || phase == BadgerPhase.SELECT || phase == BadgerPhase.CLAIM;
    }

    /** The reward the player has scrolled to, ahead of the server's echo. */
    static int selected(BadgerCrateShowEntity entity) {
        return entity.getId() == entityId ? selected : entity.selected();
    }

    /** Where the camera is, in world coordinates, and where it looks: x, y, z, yaw, pitch. */
    public static double @Nullable [] cameraPose(float partialTick) {
        if (!cameraLocked()) {
            return null;
        }
        // The track is placed on the fixed origin, not the entity's interpolated position, and its time only moves on:
        // a frame whose partial tick has wrapped before the entity's tick ran, or the entity briefly untracked,
        // would otherwise step the camera back a tick, which showed as a dip and recovery.
        BadgerCrateShowEntity entity = entity();
        if (entity != null) {
            cameraTicks = Math.max(cameraTicks, entity.tickCount + partialTick);
        }
        return pose(cameraTicks);
    }

    private static double[] pose(float ticks) {
        Vec3 eye = BadgerCrateCamera.position(ticks).add(BadgerCrateShowEntity.ORIGIN);
        return new double[] {eye.x, eye.y, eye.z, BadgerCrateCamera.yaw(ticks), BadgerCrateCamera.pitch(ticks)};
    }

    private static @Nullable BadgerCrateShowEntity entity() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && entityId >= 0 && minecraft.level.getEntity(entityId) instanceof BadgerCrateShowEntity show ? show : null;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (!active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        phaseTicks++;
        fadeO = fade;
        float target = phase == BadgerPhase.FADE_OUT || phase == BadgerPhase.LOADING || phase == BadgerPhase.CLAIM ? 1.0F : 0.0F;
        fade = Mth.approach(fade, target, 1.0F / FADE_TICKS);
        if (phase == BadgerPhase.END && fade <= 0.0F) {
            phase = -1;
            return;
        }
        if (player == null) {
            return;
        }
        // Keep the player at the camera, so the chunks around it load and nothing drifts while spectating.
        if (cameraLocked()) {
            BadgerCrateShowEntity current = entity();
            double[] pose = pose(current != null ? Math.max(cameraTicks, current.tickCount) : cameraTicks);
            player.setPos(pose[0], pose[1] - player.getEyeHeight(), pose[2]);
            player.setDeltaMovement(Vec3.ZERO);
            player.setYRot((float) pose[3]);
            player.setXRot((float) pose[4]);
        }
        BadgerCrateShowEntity show = entity();
        if (show != null && (phase == BadgerPhase.SHOW || phase == BadgerPhase.SELECT)) {
            BadgerCrateSounds.play(soundTick, show.tickCount);
            soundTick = show.tickCount;
        }
        if (phase == BadgerPhase.LOADING && !readySent && phaseTicks >= READY_DELAY && minecraft.gui.screen() == null
                && minecraft.levelRenderer.hasRenderedAllSections()) {
            readySent = true;
            ApeiraClientNetwork.sendToServer(new BadgerInputPayload(BadgerInputPayload.READY, 0));
        }
        // Swallow the keys a spectator could still use: the hotbar and spectator menu, the inventory, dropping.
        consume(minecraft.options.keyInventory);
        consume(minecraft.options.keyDrop);
        consume(minecraft.options.keySwapOffhand);
        for (KeyMapping hotbar : minecraft.options.keyHotbarSlots) {
            consume(hotbar);
        }
        boolean sneak = minecraft.options.keyShift.isDown();
        if (phase == BadgerPhase.SELECT && sneak && !sneakWasDown) {
            ApeiraClientNetwork.sendToServer(new BadgerInputPayload(BadgerInputPayload.CONFIRM, selected));
        }
        sneakWasDown = sneak;
    }

    private static void consume(KeyMapping key) {
        while (key.consumeClick()) {
            // Dropped.
        }
    }

    private static void onMovementInput(MovementInputUpdateEvent event) {
        if (cameraLocked()) {
            event.getInput().keyPresses = Input.EMPTY;
        }
    }

    private static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!active()) {
            return;
        }
        event.setCanceled(true);
        if (phase != BadgerPhase.SELECT || event.getScrollDeltaY() == 0) {
            return;
        }
        selected = Math.floorMod(selected + (event.getScrollDeltaY() > 0 ? -1 : 1), 3);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, 1.0F, 1.0F);
        }
        ApeiraClientNetwork.sendToServer(new BadgerInputPayload(BadgerInputPayload.SELECT, selected));
    }

    /** A fixed 60 degree field of view while the camera follows the recording, whatever the player's setting. */
    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (cameraLocked()) {
            event.setFOV(FOV);
        }
    }

    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Only the opening's own layer is drawn while it runs. */
    private static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (active() && !event.getName().equals(LAYER)) {
            event.setCanceled(true);
        }
    }

    /** No inventory or other screens mid-opening, only the pause menu. */
    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (cameraLocked() && event.getNewScreen() != null && !(event.getNewScreen() instanceof PauseScreen)
                && !event.getNewScreen().getClass().getName().contains("Loading")
                && !event.getNewScreen().getClass().getName().contains("ReceivingLevel")) {
            event.setCanceled(true);
        }
    }

    /** Keeps the dimension change's loading screen black, so the fade is unbroken. */
    private static void onScreenRender(ScreenEvent.Render.Post event) {
        if (active() && fade > 0.0F && !(event.getScreen() instanceof PauseScreen)) {
            GuiGraphicsExtractor graphics = event.getGuiGraphics();
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), black(fade));
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        phase = -1;
        entityId = -1;
        cameraTicks = 0.0F;
        fade = 0.0F;
        fadeO = 0.0F;
        BadgerCrateFx.release();
    }

    private static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, BadgerCrateClient::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        if (phase == BadgerPhase.SHOW) {
            bossBarPill(graphics, font, width, FIRST_BAR_Y, "message.apeira.badger_crate.opening", GREY);
        } else if (phase == BadgerPhase.SELECT) {
            bossBarPill(graphics, font, width, FIRST_BAR_Y, "message.apeira.badger_crate.collect", GREEN);
            bossBarPill(graphics, font, width, SECOND_BAR_Y, "message.apeira.badger_crate.scroll", GREY);
        }
        float alpha = Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), fadeO, fade);
        if (alpha > 0.0F) {
            graphics.fill(0, 0, width, graphics.guiHeight(), black(alpha));
        }
    }

    private static int black(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24;
    }

    /**
     * One of the server's boss bar hints as Ren's HUD capture shows them: the text in a top bar pill
     * ({@link RealmTopBar#pill}, Origin Realms' {@code boss_bar_1} and {@code boss_bar_2} glyphs), centred where vanilla
     * writes the first or second boss bar's name.
     */
    private static void bossBarPill(GuiGraphicsExtractor graphics, Font font, int screenWidth, int y, String key, int color) {
        MutableComponent bar = Component.empty();
        RealmTopBar.pill(font, bar, Component.translatable(key).withStyle(RealmTopBar.FONT.withColor(color)), true);
        graphics.text(font, bar, screenWidth / 2 - font.width(bar) / 2, y, WHITE, true);
    }

    private static void registerDecorations(RegisterItemDecorationsEvent event) {
        event.register(MainBadgerCrate.BADGER_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.COSMETIC_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.FUN_FAIR_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.GARDEN_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.KITCHEN_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.MEDIEVAL_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.MUSIC_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.SPACE_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.MINING_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.WESTERN_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.WIZARDRY_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.FAST_FOOD_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.CONSTRUCTION_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.BUSINESS_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.CAMPING_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.ANIMAL_CRATE.get(), BadgerCrateClient::decorate);
        event.register(MainBadgerCrate.BATHROOM_CRATE.get(), BadgerCrateClient::decorate);
    }

    /**
     * The hold bar: the slot fills with the cooldown overlay's white from the bottom up while the crate is held, the
     * ender pearl cooldown turned upside down.
     */
    private static boolean decorate(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || !isHeld(player, stack)) {
            return false;
        }
        float progress = Mth.clamp((BadgerCrateItem.HOLD_TICKS - player.getUseItemRemainingTicks()
                + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) / BadgerCrateItem.HOLD_TICKS, 0.0F, 1.0F);
        int height = Mth.ceil(16.0F * progress);
        if (height > 0) {
            graphics.fill(x, y + 16 - height, x + 16, y + 16, HOLD_OVERLAY);
        }
        return false;
    }

    private static boolean isHeld(LocalPlayer player, ItemStack stack) {
        ItemStack used = player.getUseItem();
        if (used == stack) {
            return true;
        }
        InteractionHand hand = player.getUsedItemHand();
        return player.getItemInHand(hand) == stack;
    }

    /** {@code BadgerSession}'s phases, as sent in {@link BadgerPhasePayload}. */
    static final class BadgerPhase {
        static final int FADE_OUT = 0;
        static final int LOADING = 1;
        static final int SHOW = 2;
        static final int SELECT = 3;
        static final int CLAIM = 4;
        static final int END = 5;

        private BadgerPhase() {}
    }
}
