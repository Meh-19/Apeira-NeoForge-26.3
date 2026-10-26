package com.apeira.decor.cosmetic;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * How a back cosmetic sits on the body, on top of {@code client/CosmeticLayer}'s fixed back transform: a move in the
 * cosmetic's own model pixels (x across, y up, z out of the back, as in Blockbench), a turn about each axis in degrees
 * (x, then y, then z, about the moved origin) and a scale. Tuned live with apeira-dev's dev wand ({@link BackOffsets}).
 */
public record BackOffset(float x, float y, float z, float pitch, float yaw, float roll, float scale) {
    public static final BackOffset DEFAULT = new BackOffset(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
    /** The farthest a cosmetic may be moved, in its model pixels. */
    public static final float MAX_OFFSET = 32.0F;

    public static final StreamCodec<ByteBuf, BackOffset> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BackOffset::x,
            ByteBufCodecs.FLOAT, BackOffset::y,
            ByteBufCodecs.FLOAT, BackOffset::z,
            ByteBufCodecs.FLOAT, BackOffset::pitch,
            ByteBufCodecs.FLOAT, BackOffset::yaw,
            ByteBufCodecs.FLOAT, BackOffset::roll,
            ByteBufCodecs.FLOAT, BackOffset::scale,
            BackOffset::new);

    /** Kept in range, as the server takes it from a client: moves within {@link #MAX_OFFSET}, turns -180 to 180, scale 0.05 to 8. */
    public BackOffset sanitized() {
        return new BackOffset(offset(x), offset(y), offset(z), Mth.wrapDegrees(pitch), Mth.wrapDegrees(yaw), Mth.wrapDegrees(roll),
                Float.isFinite(scale) ? Mth.clamp(scale, 0.05F, 8.0F) : 1.0F);
    }

    private static float offset(float value) {
        return Float.isFinite(value) ? Mth.clamp(value, -MAX_OFFSET, MAX_OFFSET) : 0.0F;
    }
}
