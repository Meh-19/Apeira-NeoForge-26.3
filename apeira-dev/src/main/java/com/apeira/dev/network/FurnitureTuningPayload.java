package com.apeira.dev.network;

import java.util.Optional;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.decor.furniture.FurnitureTuning;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A furniture piece's tuning, or its reset to the catalogue's values when empty. Client to server it is an edit from
 * the dev wand; server to client it is the tuning everyone now has.
 *
 * @param block  the furniture block's id
 * @param tuning the new tuning, or empty for the catalogue's values
 */
public record FurnitureTuningPayload(Identifier block, Optional<FurnitureTuning> tuning) implements ApeiraPayload<FurnitureTuningPayload> {
    public static final ApeiraPayloadType<FurnitureTuningPayload> TYPE = ApeiraPayloadType.of("furniture_tuning",
            StreamCodec.composite(Identifier.STREAM_CODEC, FurnitureTuningPayload::block,
                    ByteBufCodecs.optional(FurnitureTuning.STREAM_CODEC), FurnitureTuningPayload::tuning,
                    FurnitureTuningPayload::new));

    @Override
    public ApeiraPayloadType<FurnitureTuningPayload> payloadType() {
        return TYPE;
    }
}
