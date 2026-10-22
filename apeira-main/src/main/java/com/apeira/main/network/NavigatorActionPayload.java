package com.apeira.main.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.main.navigator.Navigator;
import com.apeira.main.navigator.NavigatorAction;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a navigator button was clicked; {@code index} picks an open world for {@link NavigatorAction#OPEN_WORLD}. */
public record NavigatorActionPayload(NavigatorAction action, int index) implements ApeiraPayload<NavigatorActionPayload> {
    public static final ApeiraPayloadType<NavigatorActionPayload> TYPE = ApeiraPayloadType.of("navigator_action",
            StreamCodec.composite(NavigatorAction.STREAM_CODEC, NavigatorActionPayload::action, ByteBufCodecs.VAR_INT,
                    NavigatorActionPayload::index, NavigatorActionPayload::new));

    public NavigatorActionPayload(NavigatorAction action) {
        this(action, 0);
    }

    @Override
    public ApeiraPayloadType<NavigatorActionPayload> payloadType() {
        return TYPE;
    }

    public static void handle(NavigatorActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Navigator.handle(player, payload);
        }
    }
}
