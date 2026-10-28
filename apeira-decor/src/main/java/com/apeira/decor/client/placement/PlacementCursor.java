package com.apeira.decor.client.placement;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.placement.PlacementReach;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one placement editor every "pick a spot in the world" tool builds on (the realm spawn balloon, NPC anchors): a box
 * the size of {@link #footprint} on the block surface under the crosshair, within {@link PlacementReach#CURSOR} blocks
 * and with no proximity cutoff, drawn with two selection textures, the clear one where {@link #fits} and the blocked
 * one tinted red where not. Their dashed edges have alpha 254, so they glow ({@link EmissiveRenderTypes}). The target
 * is the block under the crosshair each tick; the drawn box slides there from block to block ({@link GridGlide}).
 * Confirming only sends the spot: what it places appears there at once, with no travel animation.
 *
 * <p>The physical mouse buttons drive it, whatever attack and use are bound to: left-click {@link #confirm}s a spot that
 * fits, right-click or Escape cancels; changing level, {@link #stillValid} turning false or leaving the server drops it.
 * While it is on, attack, item use and pick-block do nothing else. The HUD under the crosshair reads
 * {@code <hudKey>.aim|clear|blocked|confirm|cancel}. Register each cursor once with {@link #register} and draw its HUD
 * with {@link #renderHud} as a GUI layer.
 */
public abstract class PlacementCursor {
    /** Tints the grey blocked texture red. */
    private static final int BLOCKED_TINT = 0xFFFF4A4A;
    /** Lifts the box off the surface it stands on, so its floor does not flicker against it. */
    private static final float LIFT = 0.005F;

    private final Identifier clearTexture;
    private final Identifier blockedTexture;
    private final String hudKey;
    private boolean active;
    private @Nullable ClientLevel level;
    private @Nullable BlockPos target;
    private boolean clear;
    private final GridGlide glide = new GridGlide();

    protected PlacementCursor(Identifier clearTexture, Identifier blockedTexture, String hudKey) {
        this.clearTexture = clearTexture;
        this.blockedTexture = blockedTexture;
        this.hudKey = hudKey;
    }

    /** The blocks the box takes when it stands on {@code target}. */
    protected abstract AABB footprint(BlockPos target);

    /** Whether the placed thing fits on {@code target} (the server checks again). */
    protected abstract boolean fits(ClientLevel level, BlockPos target);

    /** Sends the picked spot to the server. */
    protected abstract void confirm(BlockPos target);

    /** Whether the thing being placed is still there to place; false drops the cursor. */
    protected boolean stillValid(Minecraft minecraft) {
        return true;
    }

    /** Clears what the subclass keeps about this placement. */
    protected void onStop() {}

    /** Turns the cursor on in the current level and closes any open screen. */
    protected final void start() {
        Minecraft minecraft = Minecraft.getInstance();
        active = true;
        level = minecraft.level;
        target = null;
        clear = false;
        glide.reset();
        if (minecraft.gui.screen() != null) {
            minecraft.gui.setScreen(null);
        }
    }

    public final boolean isActive() {
        return active;
    }

    /** Whether the cursor is on a spot where the thing fits. */
    public final boolean isClear() {
        return active && target != null && clear;
    }

    public final void stop() {
        active = false;
        level = null;
        target = null;
        glide.reset();
        onStop();
    }

    /** Adds this cursor's game event listeners. */
    public final void register() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onMouseButton);
        NeoForge.EVENT_BUS.addListener(this::onInteraction);
        NeoForge.EVENT_BUS.addListener(this::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(this::onSubmitGeometry);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level != level || minecraft.player.isSpectator()
                || !stillValid(minecraft)) {
            stop();
            return;
        }
        target = null;
        HitResult hit = minecraft.player.pick(PlacementReach.CURSOR, 1.0F, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            // The box stands on the surface looked at: on top of the block, beside it, or under it hanging down.
            int height = (int) Math.ceil(footprint(BlockPos.ZERO).maxY);
            target = block.getDirection() == Direction.DOWN ? block.getBlockPos().below(height)
                    : block.getBlockPos().relative(block.getDirection());
            clear = fits(minecraft.level, target);
        }
    }

    private void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        int button = event.getButton();
        if (!active || minecraft.gui.screen() != null
                || (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT)) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        BlockPos pos = target;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            stop();
        } else if (pos != null && clear) {
            confirm(pos);
            stop();
        } else if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable(hudKey + ".blocked"));
        }
    }

    private void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Escape cancels instead of opening the pause menu. */
    private void onScreenOpening(ScreenEvent.Opening event) {
        if (active && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            stop();
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    /** Draws the box sliding towards the target block, camera-relative, through the emissive translucent entity type (no culling: seen from inside too). */
    private void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        BlockPos pos = target;
        if (!active || pos == null) {
            glide.reset();
            return;
        }
        AABB box = footprint(pos);
        Vec3 corner = glide.toward(new Vec3(box.minX, box.minY, box.minZ));
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(corner.x - camera.x, corner.y + LIFT - camera.y, corner.z - camera.z);
        float size = (float) box.getXsize();
        float height = (float) box.getYsize();
        int color = clear ? 0xFFFFFFFF : BLOCKED_TINT;
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack,
                EmissiveRenderTypes.entityTranslucent(clear ? clearTexture : blockedTexture),
                (pose, buffer) -> box(pose, buffer, size, height, color));
        poseStack.popPose();
    }

    /** A box {@code s} wide and deep and {@code h} high from the pose's origin, each face one copy of the texture. */
    public static void box(PoseStack.Pose pose, VertexConsumer buffer, float s, float h, int color) {
        // Bottom, top, north, south, west, east; each face's texture once, its dashed edge along the box's edges.
        face(pose, buffer, color, 0, -1, 0, 0, 0, 0, s, 0, 0, s, 0, s, 0, 0, s);
        face(pose, buffer, color, 0, 1, 0, 0, h, s, s, h, s, s, h, 0, 0, h, 0);
        face(pose, buffer, color, 0, 0, -1, s, 0, 0, 0, 0, 0, 0, h, 0, s, h, 0);
        face(pose, buffer, color, 0, 0, 1, 0, 0, s, s, 0, s, s, h, s, 0, h, s);
        face(pose, buffer, color, -1, 0, 0, 0, 0, 0, 0, 0, s, 0, h, s, 0, h, 0);
        face(pose, buffer, color, 1, 0, 0, s, 0, s, s, 0, 0, s, h, 0, s, h, s);
    }

    private static void face(PoseStack.Pose pose, VertexConsumer buffer, int color, float nx, float ny, float nz, float x0, float y0, float z0,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(pose, buffer, color, nx, ny, nz, x0, y0, z0, 0, 1);
        vertex(pose, buffer, color, nx, ny, nz, x1, y1, z1, 1, 1);
        vertex(pose, buffer, color, nx, ny, nz, x2, y2, z2, 1, 0);
        vertex(pose, buffer, color, nx, ny, nz, x3, y3, z3, 0, 0);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int color, float nx, float ny, float nz, float x, float y, float z,
            float u, float v) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    /** The instructions under the crosshair while placing, and whether the spot is clear. */
    public final void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.gui.hud.isHidden()) {
            return;
        }
        Font font = minecraft.font;
        String state = target == null ? ".aim" : clear ? ".clear" : ".blocked";
        Component[] lines = {
                Component.translatable(hudKey + state),
                Component.translatable(hudKey + ".confirm"),
                Component.translatable(hudKey + ".cancel")};
        int[] colors = {isClear() ? 0xFF9FC544 : 0xFFF17F61, 0xFFFFFFFF, 0xFFA7AEBA};
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int lineHeight = font.lineHeight + 2;
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 16;
        graphics.fill(x - width / 2 - 4, y - 3, x + (width + 1) / 2 + 4, y + lines.length * lineHeight + 1, 0x80000000);
        for (int i = 0; i < lines.length; i++) {
            graphics.centeredText(font, lines[i], x, y + i * lineHeight, colors[i]);
        }
    }
}
