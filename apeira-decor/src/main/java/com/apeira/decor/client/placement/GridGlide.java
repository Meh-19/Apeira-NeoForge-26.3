package com.apeira.decor.client.placement;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * Eases a placement cursor's drawn position towards the grid position it is on, so the box slides from block to block
 * instead of snapping: each frame it closes {@code 1 - e^(-dt / TIME_CONSTANT_MS)} of the gap (frame-rate independent,
 * about 95% of a one-block step in 3 time constants). A cursor that has just appeared, or that jumps more than
 * {@link #SNAP_DISTANCE} blocks, is drawn where it is at once. Only the drawing glides; the cursor's target, fit check
 * and confirm stay on the grid.
 */
public final class GridGlide {
    /** How quickly the drawn box catches up, in milliseconds. */
    public static final float TIME_CONSTANT_MS = 40.0F;
    /** Jumps longer than this, in blocks, are not glided. */
    public static final double SNAP_DISTANCE = 12.0;

    private @Nullable Vec3 drawn;
    private long millis;

    /** The position to draw this frame for a cursor on {@code target}. */
    public Vec3 toward(Vec3 target) {
        long now = Util.getMillis();
        Vec3 from = drawn;
        if (from == null || from.distanceToSqr(target) > SNAP_DISTANCE * SNAP_DISTANCE) {
            drawn = target;
        } else {
            double t = 1.0 - Math.exp(-(now - millis) / TIME_CONSTANT_MS);
            drawn = from.distanceToSqr(target) < 1.0E-6 ? target : from.lerp(target, t);
        }
        millis = now;
        return drawn;
    }

    /** Forgets the drawn position, so the cursor's next appearance is not glided from its last one. */
    public void reset() {
        drawn = null;
    }
}
