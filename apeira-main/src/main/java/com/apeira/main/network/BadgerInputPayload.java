package com.apeira.main.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.main.crate.BadgerSessions;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server, during a Badger Crate opening: {@link #READY} (the instance's chunks around the camera are drawn),
 * {@link #SELECT} ({@code value} is the reward scrolled to, 0 to 2) or {@link #CONFIRM} (crouched to collect). The
 * server checks each against the opener's session.
 */
public record BadgerInputPayload(int action, int value) implements ApeiraPayload<BadgerInputPayload> {
    public static final int READY = 0;
    public static final int SELECT = 1;
    public static final int CONFIRM = 2;
    public static final StreamCodec<ByteBuf, BadgerInputPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BadgerInputPayload::action,
            ByteBufCodecs.VAR_INT, BadgerInputPayload::value,
            BadgerInputPayload::new);
    public static final ApeiraPayloadType<BadgerInputPayload> TYPE = ApeiraPayloadType.of("badger_input", CODEC);

    @Override
    public ApeiraPayloadType<BadgerInputPayload> payloadType() {
        return TYPE;
    }

    public static void handle(BadgerInputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BadgerSessions.input(player, payload.action(), payload.value());
        }
    }
}
