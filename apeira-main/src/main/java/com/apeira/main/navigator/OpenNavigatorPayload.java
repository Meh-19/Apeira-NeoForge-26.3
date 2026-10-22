package com.apeira.main.navigator;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: show the navigator on {@code page}, opening it if it is closed. Sent by {@code /navigator} and in
 * answer to {@link NavigatorActionPayload}s that change page. Handled in {@code client/ApeiraClient}.
 */
public record OpenNavigatorPayload(NavigatorPage page) implements ApeiraPayload<OpenNavigatorPayload> {
    public static final ApeiraPayloadType<OpenNavigatorPayload> TYPE = ApeiraPayloadType.of("open_navigator",
            StreamCodec.composite(NavigatorPage.STREAM_CODEC, OpenNavigatorPayload::page, OpenNavigatorPayload::new));

    @Override
    public ApeiraPayloadType<OpenNavigatorPayload> payloadType() {
        return TYPE;
    }
}
