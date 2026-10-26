package com.apeira.dev.network;

import java.util.Optional;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.decor.cosmetic.BackOffset;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A back cosmetic's offset, or its reset to the fixed back transform when empty. Client to server it is an edit from
 * the dev wand; server to client it is the offset everyone now has.
 *
 * @param item   the cosmetic item's id
 * @param offset the new offset, or empty for none
 */
public record BackOffsetPayload(Identifier item, Optional<BackOffset> offset) implements ApeiraPayload<BackOffsetPayload> {
    public static final ApeiraPayloadType<BackOffsetPayload> TYPE = ApeiraPayloadType.of("back_offset",
            StreamCodec.composite(Identifier.STREAM_CODEC, BackOffsetPayload::item,
                    ByteBufCodecs.optional(BackOffset.STREAM_CODEC), BackOffsetPayload::offset,
                    BackOffsetPayload::new));

    @Override
    public ApeiraPayloadType<BackOffsetPayload> payloadType() {
        return TYPE;
    }
}
