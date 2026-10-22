package com.apeira.main.realm;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the player held F on {@code preset} in the onboarding screen. The server answers with a {@link RealmCreatedPayload}. */
public record SelectRealmPresetPayload(RealmPreset preset) implements ApeiraPayload<SelectRealmPresetPayload> {
    public static final ApeiraPayloadType<SelectRealmPresetPayload> TYPE = ApeiraPayloadType.of("select_realm_preset",
            StreamCodec.composite(RealmPreset.STREAM_CODEC, SelectRealmPresetPayload::preset, SelectRealmPresetPayload::new));

    @Override
    public ApeiraPayloadType<SelectRealmPresetPayload> payloadType() {
        return TYPE;
    }

    public static void handle(SelectRealmPresetPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
            RealmPool.claim(player, payload.preset);
        }
    }
}
