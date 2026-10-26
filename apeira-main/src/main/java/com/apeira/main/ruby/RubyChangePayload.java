package com.apeira.main.ruby;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server to client: the player's ruby balance just changed by {@code delta} to {@code balance}, sent by {@link Rubies#set}
 * for every direct balance change (payments, deposits, rewards, operator commands), never for ruby items. Only the HUD's
 * ruby ticker ({@code client/ruby/RubyTickerAnimation}) reads it; the balance itself still arrives as the synced attachment.
 */
public record RubyChangePayload(long delta, long balance) implements ApeiraPayload<RubyChangePayload> {
    public static final ApeiraPayloadType<RubyChangePayload> TYPE = ApeiraPayloadType.of("ruby_change", StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, RubyChangePayload::delta,
            ByteBufCodecs.VAR_LONG, RubyChangePayload::balance,
            RubyChangePayload::new));

    @Override
    public ApeiraPayloadType<RubyChangePayload> payloadType() {
        return TYPE;
    }

    static void send(ServerPlayer player, long delta, long balance) {
        ApeiraPackets.sendToPlayer(player, new RubyChangePayload(delta, balance));
    }
}
