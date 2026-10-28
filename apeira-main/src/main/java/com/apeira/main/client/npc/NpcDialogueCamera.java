package com.apeira.main.client.npc;

import com.apeira.main.npc.NpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Turns the player to the NPC they are talking to and zooms in a little while its dialogue is open: over
 * {@link #PAN_MS} the view eases (in and out) from where the player looked to the NPC's head bone as its animation
 * draws it this frame ({@link BedrockNpcRenderer#head}), then follows that bone as the animation moves it; the field of view narrows by {@link #ZOOM} over {@link #ZOOM_MS} and widens back as smoothly when the
 * dialogue closes. The player's own rotation follows the view each frame, so closing leaves them looking at the NPC.
 */
public final class NpcDialogueCamera {
    private static final float PAN_MS = 600.0F;
    private static final float ZOOM_MS = 400.0F;
    /** How much narrower the field of view gets: 15%. */
    private static final float ZOOM = 0.15F;

    private static int npc = -1;
    private static long started;
    private static long released;
    private static float startYaw;
    private static float startPitch;

    private NpcDialogueCamera() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NpcDialogueCamera::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(NpcDialogueCamera::onComputeFov);
    }

    /** Locks the view on the NPC with entity id {@code id}. */
    static void focus(int id) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // A new dialogue while the last one's zoom is still easing out starts from that zoom.
        long now = Util.getMillis();
        float zoom = zoom(now);
        npc = id;
        started = now - (long) (zoom * ZOOM_MS);
        startYaw = player.getYRot();
        startPitch = player.getXRot();
    }

    /** Frees the view; the zoom eases back out. */
    static void release() {
        if (npc >= 0) {
            float zoom = zoom(Util.getMillis());
            npc = -1;
            released = Util.getMillis() - (long) ((1.0F - zoom) * ZOOM_MS);
        }
    }

    /** The NPC being talked to, or null. */
    static NpcEntity target() {
        Minecraft minecraft = Minecraft.getInstance();
        if (npc < 0 || minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(npc);
        return entity instanceof NpcEntity found && found.isAlive() ? found : null;
    }

    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        NpcEntity target = target();
        if (player == null || target == null) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        Vec3 delta = BedrockNpcRenderer.head(target, partialTick).subtract(player.getEyePosition(partialTick));
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
        float t = ease(Math.min(1.0F, (Util.getMillis() - started) / PAN_MS));
        yaw = startYaw + t * Mth.wrapDegrees(yaw - startYaw);
        pitch = Mth.lerp(t, startPitch, pitch);
        event.setYaw(yaw);
        event.setPitch(pitch);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        float zoom = zoom(Util.getMillis());
        if (zoom > 0.0F) {
            event.setFOV(event.getFOV() * (1.0F - ZOOM * zoom));
        }
    }

    /** The zoom's progress, 0 (none) to 1 (fully in), eased. */
    private static float zoom(long now) {
        if (npc >= 0) {
            return ease(Math.min(1.0F, (now - started) / ZOOM_MS));
        }
        return 1.0F - ease(Math.min(1.0F, (now - released) / ZOOM_MS));
    }

    private static float ease(float t) {
        return t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
}
