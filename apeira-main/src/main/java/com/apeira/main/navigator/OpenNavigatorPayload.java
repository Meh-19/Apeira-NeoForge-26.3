package com.apeira.main.navigator;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: show {@code view}, opening the navigator if it is closed, or close it when the view's page is
 * {@link NavigatorPage#CLOSED}. Sent by {@code /navigator}, {@code /realm} commands and in answer to every
 * {@link NavigatorActionPayload}. Handled in {@code client/ApeiraClient}.
 */
public record OpenNavigatorPayload(NavigatorView view) implements ApeiraPayload<OpenNavigatorPayload> {
    public static final ApeiraPayloadType<OpenNavigatorPayload> TYPE = ApeiraPayloadType.of("open_navigator",
            StreamCodec.composite(NavigatorView.STREAM_CODEC, OpenNavigatorPayload::view, OpenNavigatorPayload::new));

    @Override
    public ApeiraPayloadType<OpenNavigatorPayload> payloadType() {
        return TYPE;
    }
}
