package com.apeira.decor.client;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * A short glide from where something was to where it was just placed, so a moved model eases into its new spot instead
 * of snapping there: renderers draw it at {@link #offset()} from its real position (and turned by {@link #yaw}), which
 * runs from the old position to nothing over {@link #DURATION_MS} with an ease-out curve and a small hop in the middle.
 * Client side only; the server has already moved the thing.
 */
public final class PlacementGlide {
    /** How long the glide takes. */
    public static final long DURATION_MS = 650;
    /** How high, in blocks, the glide arcs over the straight line at its middle. */
    private static final double HOP = 0.4;

    private final Vec3 from;
    private final Vec3 to;
    private final float fromYaw;
    private final long start;

    private PlacementGlide(Vec3 from, Vec3 to, float fromYaw) {
        this.from = from;
        this.to = to;
        this.fromYaw = fromYaw;
        this.start = Util.getMillis();
    }

    /** A glide from {@code from} (facing {@code fromYaw}) to {@code to}, starting now. */
    public static PlacementGlide between(Vec3 from, Vec3 to, float fromYaw) {
        return new PlacementGlide(from, to, fromYaw);
    }

    public Vec3 from() {
        return from;
    }

    public Vec3 to() {
        return to;
    }

    /** How far along the glide is, 0 to 1. */
    public float progress() {
        return Mth.clamp((Util.getMillis() - start) / (float) DURATION_MS, 0.0F, 1.0F);
    }

    public boolean done() {
        return progress() >= 1.0F;
    }

    /** Ease-out cubic: fast off the old spot, settling gently into the new one. */
    private static double ease(float t) {
        float u = 1.0F - t;
        return 1.0 - u * u * u;
    }

    /** Where to draw the model, relative to its real (new) position. */
    public Vec3 offset() {
        float t = progress();
        double rest = 1.0 - ease(t);
        return from.subtract(to).scale(rest).add(0.0, HOP * Mth.sin(t * Mth.PI), 0.0);
    }

    /** The yaw to draw, eased from the old yaw to {@code toYaw} the short way round. */
    public float yaw(float toYaw) {
        return Mth.rotLerp((float) ease(progress()), fromYaw, toYaw);
    }
}
