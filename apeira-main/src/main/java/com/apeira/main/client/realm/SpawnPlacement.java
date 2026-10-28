package com.apeira.main.client.realm;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.main.realm.ConfirmSpawnPayload;
import com.apeira.main.realm.SpawnPlacementPayload;
import com.apeira.main.realm.balloon.StarterBalloon;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The realm spawn placement cursor, started by {@link SpawnPlacementPayload} ({@code /realm setspawn}, the realm
 * settings' Set Spawn): a 3x3x2 box on the block surface under the crosshair, where the starter balloon's basket would
 * stand, drawn with Ren's selection textures: green ({@code textures/misc/spawn_selection.png}) where the balloon fits,
 * red (the grey {@code spawn_selection_blocked.png} tinted) where something is in the way or it would leave the border
 * ({@link StarterBalloon#canPlace}, which the server checks again). Their dashed edges have alpha 254, so they glow in
 * the dark ({@link EmissiveRenderTypes}). Left-click sends {@link ConfirmSpawnPayload}; right-click or Escape cancels;
 * changing level or leaving the server drops it. While it is on, attack, use and pick-block do nothing else.
 *
 * <p>The box is a ghost that only shows near the player: it fades out as the player's bounding box gets from
 * {@link #PREVIEW_CUTOFF} less {@link #PREVIEW_FADE} to {@link #PREVIEW_CUTOFF} blocks away from it and is gone, and
 * cannot be confirmed, beyond that ({@link #previewAlpha}).
 */
public final class SpawnPlacement {
    private static final Identifier CLEAR_TEXTURE = ApeiraIds.id("textures/misc/spawn_selection.png");
    private static final Identifier BLOCKED_TEXTURE = ApeiraIds.id("textures/misc/spawn_selection_blocked.png");
    /** Tints the grey blocked texture red. */
    private static final int BLOCKED_TINT = 0xFFFF4A4A;
    /** Lifts the box off the surface it stands on, so its floor does not flicker against it. */
    private static final float LIFT = 0.005F;
    /** How far, in blocks, the player may be from the preview box before it is gone. */
    public static final double PREVIEW_CUTOFF = 1.5;
    /** Over how many blocks, up to {@link #PREVIEW_CUTOFF}, the preview fades out. */
    public static final double PREVIEW_FADE = 0.5;

    private static boolean active;
    private static @Nullable ClientLevel level;
    private static @Nullable BlockPos balloon;
    private static @Nullable BlockPos target;
    private static boolean clear;
    /** The preview's opacity by the player's distance from it, 0 when it is too far to show. */
    private static float alpha;

    private SpawnPlacement() {}

    public static boolean isActive() {
        return active;
    }

    /** Whether the cursor is on a spot where the balloon fits. */
    public static boolean isClear() {
        return active && target != null && clear;
    }

    public static void onStart(SpawnPlacementPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        active = true;
        level = minecraft.level;
        balloon = payload.balloon().orElse(null);
        target = null;
        clear = false;
        if (minecraft.gui.screen() != null) {
            minecraft.gui.setScreen(null);
        }
    }

    private static void stop() {
        active = false;
        level = null;
        balloon = null;
        target = null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level != level || minecraft.player.isSpectator()) {
            stop();
            return;
        }
        target = null;
        HitResult hit = minecraft.player.pick(SpawnPlacementPayload.REACH, 1.0F, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            // The box stands on the surface looked at: on top of the block, or beside it on a side face.
            target = block.getDirection() == Direction.DOWN ? block.getBlockPos().below(StarterBalloon.HEIGHT)
                    : block.getBlockPos().relative(block.getDirection());
            clear = StarterBalloon.canPlace(minecraft.level, target, balloon);
            alpha = previewAlpha(minecraft.player.getBoundingBox(), bounds(target));
        }
    }

    /**
     * The physical left mouse button confirms the spot (when it fits) and the right one cancels, whatever the attack and
     * use keys are bound to; they never reach the game while placing.
     */
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
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
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            stop();
        } else if (minecraft.player != null) {
            if (target != null && clear && alpha > 0.0F) {
                ApeiraClientNetwork.sendToServer(new ConfirmSpawnPayload(target));
                stop();
            } else if (target != null && alpha <= 0.0F) {
                minecraft.player.sendOverlayMessage(Component.translatable("hud.apeira.placement.too_far"));
            } else {
                minecraft.player.sendOverlayMessage(Component.translatable("hud.apeira.setspawn.blocked"));
            }
        }
    }

    /** Attacks, item use and pick-block, from whatever keys they are bound to, do nothing while placing. */
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Escape cancels instead of opening the pause menu. */
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (active && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            stop();
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    /** Draws the box, camera-relative, through the emissive translucent entity type (no culling: seen from inside too). */
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        BlockPos pos = target;
        if (!active || pos == null || alpha <= 0.0F) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - StarterBalloon.RADIUS - camera.x, pos.getY() + LIFT - camera.y,
                pos.getZ() - StarterBalloon.RADIUS - camera.z);
        float size = StarterBalloon.RADIUS * 2 + 1;
        float height = StarterBalloon.HEIGHT;
        int color = withAlpha(clear ? 0xFFFFFFFF : BLOCKED_TINT, alpha);
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack,
                EmissiveRenderTypes.entityTranslucent(clear ? CLEAR_TEXTURE : BLOCKED_TEXTURE),
                (pose, buffer) -> box(pose, buffer, size, height, color));
        poseStack.popPose();
    }

    /** The blocks the balloon's box takes when it stands on {@code target}. */
    private static AABB bounds(BlockPos target) {
        return new AABB(target.getX() - StarterBalloon.RADIUS, target.getY(), target.getZ() - StarterBalloon.RADIUS,
                target.getX() + StarterBalloon.RADIUS + 1, target.getY() + StarterBalloon.HEIGHT, target.getZ() + StarterBalloon.RADIUS + 1);
    }

    /**
     * The placement preview's opacity for a player whose bounding box is {@code player}: 1 within
     * {@link #PREVIEW_CUTOFF} less {@link #PREVIEW_FADE} blocks of {@code bounds}, fading to 0 at {@link #PREVIEW_CUTOFF}.
     */
    public static float previewAlpha(AABB player, AABB bounds) {
        double distance = Math.sqrt(bounds.distanceToSqr(player));
        return (float) Mth.clamp((PREVIEW_CUTOFF - distance) / PREVIEW_FADE, 0.0, 1.0);
    }

    /** {@code color} with its alpha scaled by {@code alpha}. */
    public static int withAlpha(int color, float alpha) {
        return Math.round((color >>> 24) * alpha) << 24 | color & 0xFFFFFF;
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

    /** The instructions over the hotbar while placing, and whether the spot is clear. */
    public static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.gui.hud.isHidden()) {
            return;
        }
        Font font = minecraft.font;
        Component status = target == null ? Component.translatable("hud.apeira.setspawn.aim")
                : alpha <= 0.0F ? Component.translatable("hud.apeira.placement.too_far")
                : clear ? Component.translatable("hud.apeira.setspawn.clear") : Component.translatable("hud.apeira.setspawn.blocked");
        Component[] lines = {
                status,
                Component.translatable("hud.apeira.setspawn.confirm"),
                Component.translatable("hud.apeira.setspawn.cancel")};
        int[] colors = {target != null && clear && alpha > 0.0F ? 0xFF9FC544 : 0xFFF17F61, 0xFFFFFFFF, 0xFFA7AEBA};
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
