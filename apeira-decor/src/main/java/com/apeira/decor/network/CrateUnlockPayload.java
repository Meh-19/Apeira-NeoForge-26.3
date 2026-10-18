package com.apeira.decor.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

/** Server to client: the player's crate opening ended (collected, or they walked away). The client frees the camera. */
public record CrateUnlockPayload() implements ApeiraPayload<CrateUnlockPayload> {
    public static final CrateUnlockPayload INSTANCE = new CrateUnlockPayload();
    public static final ApeiraPayloadType<CrateUnlockPayload> TYPE = ApeiraPayloadType.unit("crate_unlock", INSTANCE);

    @Override
    public ApeiraPayloadType<CrateUnlockPayload> payloadType() {
        return TYPE;
    }
}
