package com.apeira.main.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.main.navigator.NavigatorPage;

import net.minecraft.network.codec.StreamCodec;

/** Server to client: opens the navigator on {@code page}, or switches an open navigator to it. */
public record OpenNavigatorPayload(NavigatorPage page) implements ApeiraPayload<OpenNavigatorPayload> {
    public static final ApeiraPayloadType<OpenNavigatorPayload> TYPE = ApeiraPayloadType.of("open_navigator",
            StreamCodec.composite(NavigatorPage.STREAM_CODEC, OpenNavigatorPayload::page, OpenNavigatorPayload::new));

    @Override
    public ApeiraPayloadType<OpenNavigatorPayload> payloadType() {
        return TYPE;
    }
}
