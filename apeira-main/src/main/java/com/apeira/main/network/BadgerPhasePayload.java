package com.apeira.main.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: a Badger Crate opening moved to {@code phase} ({@code com.apeira.main.crate.BadgerSession}'s
 * {@code PHASE_*}). {@code entity} is the opening's {@code BadgerCrateShowEntity} once it exists, else -1. Handled in
 * {@code client/crate/BadgerCrateClient}.
 */
public record BadgerPhasePayload(int phase, int entity) implements ApeiraPayload<BadgerPhasePayload> {
    public static final StreamCodec<ByteBuf, BadgerPhasePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BadgerPhasePayload::phase,
            ByteBufCodecs.VAR_INT, BadgerPhasePayload::entity,
            BadgerPhasePayload::new);
    public static final ApeiraPayloadType<BadgerPhasePayload> TYPE = ApeiraPayloadType.of("badger_phase", CODEC);

    @Override
    public ApeiraPayloadType<BadgerPhasePayload> payloadType() {
        return TYPE;
    }
}
