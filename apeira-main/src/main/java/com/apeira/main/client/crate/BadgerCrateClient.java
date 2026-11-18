package com.apeira.main.client.crate;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.Apeira;
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
import net.minecraft.resources.Identifier;
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
    private static final int READY_DELAY = 10;
    private static final int SELECT_TICKS = 5 * 60 * 20;
    /** The vanilla cooldown overlay's white at half alpha. */
    private static final int HOLD_OVERLAY = 0x7FFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int GREY = 0xFFAAAAAA;

    private static int phase = -1;
    private static int entityId = -1;
    private static int phaseTicks;
    private static float fade;
    private static float fadeO;
    private static boolean readySent;
    private static boolean sneakWasDown;
    private static int selected;

    private BadgerCrateClient() {}

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(BadgerCrateClient::registerLayer);
        modBus.addListener(BadgerCrateClient::registerDecorations);
        gameBus.addListener(BadgerCrateClient::onClientTick);
        gameBus.addListener(BadgerCrateClient::onMovementInput);
        gameBus.addListener(BadgerCrateClient::onScroll);
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
        }
        if (phase == BadgerPhase.LOADING) {
            readySent = false;
        }
        if (phase == BadgerPhase.END) {
            entityId = -1;
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
        float ticks = animationTicks(partialTick);
        BadgerCrateShowEntity entity = entity();
        Vec3 origin = entity != null ? entity.position() : BadgerCrateShowEntity.ORIGIN;
        Vec3 eye = BadgerCrateCamera.position(ticks).add(origin);
        return new double[] {eye.x, eye.y, eye.z, BadgerCrateCamera.yaw(ticks), BadgerCrateCamera.pitch(ticks)};
    }

    private static float animationTicks(float partialTick) {
        BadgerCrateShowEntity entity = entity();
        return entity == null ? 0.0F : entity.tickCount + partialTick;
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
        double[] pose = cameraPose(1.0F);
        if (pose != null) {
            player.setPos(pose[0], pose[1] - player.getEyeHeight(), pose[2]);
            player.setDeltaMovement(Vec3.ZERO);
            player.setYRot((float) pose[3]);
            player.setXRot((float) pose[4]);
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
        ApeiraClientNetwork.sendToServer(new BadgerInputPayload(BadgerInputPayload.SELECT, selected));
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
        int bottom = graphics.guiHeight() - 40;
        if (phase == BadgerPhase.SHOW) {
            line(graphics, font, width, bottom, Component.translatable("message.apeira.badger_crate.opening"), WHITE);
        } else if (phase == BadgerPhase.SELECT) {
            line(graphics, font, width, bottom - 12, Component.translatable("message.apeira.badger_crate.collect"), GREEN);
            int secondsLeft = Math.max(0, (SELECT_TICKS - phaseTicks) / 20);
            Component left = Component.translatable("message.apeira.badger_crate.time_left",
                    String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60));
            line(graphics, font, width, bottom, Component.translatable("message.apeira.badger_crate.scroll").copy()
                    .append(" · ").append(left), GREY);
        }
        float alpha = Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), fadeO, fade);
        if (alpha > 0.0F) {
            graphics.fill(0, 0, width, graphics.guiHeight(), black(alpha));
        }
    }

    private static int black(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24;
    }

    private static void line(GuiGraphicsExtractor graphics, Font font, int screenWidth, int y, Component text, int color) {
        int x = (screenWidth - font.width(text)) / 2;
        graphics.text(font, text, x, y, color, true);
    }

    private static void registerDecorations(RegisterItemDecorationsEvent event) {
        event.register(MainBadgerCrate.BADGER_CRATE.get(), BadgerCrateClient::decorate);
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
