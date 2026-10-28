package com.apeira.main.client.npc;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.GridGlide;
import com.apeira.main.client.realm.SpawnPlacement;
import com.apeira.main.npc.ConfirmNpcAnchorPayload;
import com.apeira.main.npc.NpcAnchorPayload;
import com.apeira.main.npc.NpcEntity;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
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
 * The NPC anchor placement cursor, started by {@link NpcAnchorPayload} (the NPC settings' Set Anchor): the realm spawn
 * cursor ({@link SpawnPlacement}) sized to the NPC's collision box ({@link NpcEntity#footprint}), standing on the block
 * surface under the crosshair, green where the NPC fits and red where not ({@link NpcEntity#canStandAt}, which the server
 * checks again). Left-click sends {@link ConfirmNpcAnchorPayload}, and the server moves the NPC there facing the player;
 * right-click or Escape cancels; changing level, the NPC leaving it or leaving the server drops it. Unlike the spawn
 * cursor's, its box has no distance cutoff: it shows and places anywhere within reach. Like it, the drawn box slides
 * from block to block ({@link GridGlide}).
 */
public final class NpcAnchorPlacement {
    private static final Identifier CLEAR_TEXTURE = ApeiraIds.id("textures/misc/spawn_selection.png");
    private static final Identifier BLOCKED_TEXTURE = ApeiraIds.id("textures/misc/spawn_selection_blocked.png");
    private static final int BLOCKED_TINT = 0xFFFF4A4A;
    private static final float LIFT = 0.005F;

    private static boolean active;
    private static @Nullable ClientLevel level;
    private static int npc;
    private static @Nullable EntityType<?> type;
    private static @Nullable BlockPos target;
    private static boolean clear;
    private static final GridGlide GLIDE = new GridGlide();

    private NpcAnchorPlacement() {}

    public static void onStart(NpcAnchorPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(payload.npc()) instanceof NpcEntity entity)) {
            return;
        }
        active = true;
        level = minecraft.level;
        npc = payload.npc();
        type = entity.getType();
        target = null;
        clear = false;
        GLIDE.reset();
        if (minecraft.gui.screen() != null) {
            minecraft.gui.setScreen(null);
        }
    }

    private static void stop() {
        active = false;
        level = null;
        type = null;
        target = null;
        GLIDE.reset();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level != level || minecraft.player.isSpectator()
                || !(minecraft.level.getEntity(npc) instanceof NpcEntity) || type == null) {
            stop();
            return;
        }
        target = null;
        HitResult hit = minecraft.player.pick(NpcAnchorPayload.REACH, 1.0F, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            // The NPC stands on the surface looked at: on top of the block, beside it, or under it hanging down.
            int height = (int) (NpcEntity.footprint(type, BlockPos.ZERO).maxY);
            target = block.getDirection() == Direction.DOWN ? block.getBlockPos().below(height)
                    : block.getBlockPos().relative(block.getDirection());
            clear = NpcEntity.canStandAt(minecraft.level, type, target);
        }
    }

    /** The physical left mouse button confirms the spot (when it fits), the right one cancels; neither reaches the game. */
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
            if (target != null && clear) {
                ApeiraClientNetwork.sendToServer(new ConfirmNpcAnchorPayload(npc, target));
                stop();
            } else {
                minecraft.player.sendOverlayMessage(Component.translatable("hud.apeira.npc_anchor.blocked"));
            }
        }
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (active && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            stop();
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    /** Draws the NPC's footprint at the target, camera-relative, as the spawn cursor draws its box. */
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        BlockPos pos = target;
        EntityType<?> npcType = type;
        if (!active || pos == null || npcType == null) {
            GLIDE.reset();
            return;
        }
        AABB box = NpcEntity.footprint(npcType, pos);
        Vec3 corner = GLIDE.toward(new Vec3(box.minX, box.minY, box.minZ));
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(corner.x - camera.x, corner.y + LIFT - camera.y, corner.z - camera.z);
        float size = (float) box.getXsize();
        float height = (float) box.getYsize();
        int color = clear ? 0xFFFFFFFF : BLOCKED_TINT;
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack,
                EmissiveRenderTypes.entityTranslucent(clear ? CLEAR_TEXTURE : BLOCKED_TEXTURE),
                (pose, buffer) -> SpawnPlacement.box(pose, buffer, size, height, color));
        poseStack.popPose();
    }

    public static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.gui.hud.isHidden()) {
            return;
        }
        Font font = minecraft.font;
        Component status = target == null ? Component.translatable("hud.apeira.npc_anchor.aim")
                : clear ? Component.translatable("hud.apeira.npc_anchor.clear") : Component.translatable("hud.apeira.npc_anchor.blocked");
        Component[] lines = {
                status,
                Component.translatable("hud.apeira.npc_anchor.confirm"),
                Component.translatable("hud.apeira.npc_anchor.cancel")};
        int[] colors = {target != null && clear ? 0xFF9FC544 : 0xFFF17F61, 0xFFFFFFFF, 0xFFA7AEBA};
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
