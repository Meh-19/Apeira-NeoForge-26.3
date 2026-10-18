package com.apeira.decor.client.crate;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.crate.CrateBlockEntity;
import com.apeira.decor.network.CrateOpenPayload;
import com.apeira.decor.network.CrateUnlockPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Locks the camera on the crate being opened, ported from CosmoLib's {@code CrateCameraController}. Over the first
 * second the view eases from where the player was looking to the crate; after that the player's own rotation is kept
 * on the crate, so punching always hits it. The player moves 25% slower and cannot scroll the hotbar until the server
 * ends the opening ({@link CrateUnlockPayload}) or the crate is gone.
 */
public final class CrateCamera {
    private static final Identifier SLOW_DOWN = ApeiraIds.id("crate_opening");
    /** How much of the pan is done each tick: 20 ticks to turn fully. */
    private static final float PAN_PER_TICK = 0.05F;

    private static @Nullable BlockPos target;
    private static float pan;
    private static float startYaw;
    private static float startPitch;

    private CrateCamera() {}

    /** The crate the camera is locked on, or {@code null}. */
    public static @Nullable BlockPos target() {
        return target;
    }

    public static void onOpen(CrateOpenPayload payload, IPayloadContext context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        target = payload.pos();
        pan = 0.0F;
        startYaw = player.getYRot();
        startPitch = player.getXRot();
        slowDown(player);
    }

    public static void onUnlock(CrateUnlockPayload payload, IPayloadContext context) {
        unlock();
    }

    /** Leaves the player looking at the crate, frees the camera and restores their speed. */
    private static void unlock() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (target != null) {
                lookAt(player, target, 1.0F);
            }
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(SLOW_DOWN);
            }
        }
        target = null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (target == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.level.getBlockEntity(target) instanceof CrateBlockEntity)) {
            unlock();
            return;
        }
        pan = Math.min(1.0F, pan + PAN_PER_TICK);
        if (pan >= 1.0F) {
            // Aim the player's real rotation at the crate, so attacks reach it wherever the mouse points.
            lookAt(player, target, 1.0F);
        }
        slowDown(player);
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (target == null || player == null) {
            return;
        }
        Vec3 eye = player.getEyePosition(event.getPartialTick());
        float yaw = yawTo(eye, target);
        float pitch = pitchTo(eye, target);
        if (pan >= 1.0F) {
            event.setYaw(yaw);
            event.setPitch(pitch);
        } else {
            float t = easeInOut(pan);
            event.setYaw(startYaw + t * Mth.wrapDegrees(yaw - startYaw));
            event.setPitch(Mth.lerp(t, startPitch, pitch));
        }
    }

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (target != null) {
            event.setCanceled(true);
        }
    }

    private static void lookAt(LocalPlayer player, BlockPos crate, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        player.setYRot(yawTo(eye, crate));
        player.setXRot(pitchTo(eye, crate));
    }

    /** Aims half a block above the crate's base centre, as CosmoLib did. */
    private static Vec3 aim(BlockPos crate) {
        return Vec3.atBottomCenterOf(crate).add(0.0, 0.5, 0.0);
    }

    private static float yawTo(Vec3 eye, BlockPos crate) {
        Vec3 delta = aim(crate).subtract(eye);
        return (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
    }

    private static float pitchTo(Vec3 eye, BlockPos crate) {
        Vec3 delta = aim(crate).subtract(eye);
        return (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
    }

    private static float easeInOut(float t) {
        return t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private static void slowDown(LocalPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SLOW_DOWN) == null) {
            speed.addTransientModifier(new AttributeModifier(SLOW_DOWN, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
