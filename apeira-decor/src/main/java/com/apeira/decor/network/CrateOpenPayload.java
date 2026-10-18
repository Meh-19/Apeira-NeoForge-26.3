package com.apeira.decor.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: the player started opening the crate at {@code pos}. The client turns the camera to the crate,
 * slows the player and shows the crate's hints until {@link CrateUnlockPayload}.
 */
public record CrateOpenPayload(BlockPos pos) implements ApeiraPayload<CrateOpenPayload> {
    public static final ApeiraPayloadType<CrateOpenPayload> TYPE = ApeiraPayloadType.of("crate_open",
            StreamCodec.composite(BlockPos.STREAM_CODEC, CrateOpenPayload::pos, CrateOpenPayload::new));

    @Override
    public ApeiraPayloadType<CrateOpenPayload> payloadType() {
        return TYPE;
    }
}
