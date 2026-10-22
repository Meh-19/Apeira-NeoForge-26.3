package com.apeira.main.realm;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

/**
 * Server to client: the picked realm is the player's and they travel there shortly ({@link RealmPool#claim}). The
 * onboarding screen shows its "Realm Created" and "Traveling Home..." cards until the player arrives.
 */
public record RealmCreatedPayload() implements ApeiraPayload<RealmCreatedPayload> {
    public static final ApeiraPayloadType<RealmCreatedPayload> TYPE = ApeiraPayloadType.unit("realm_created", new RealmCreatedPayload());

    @Override
    public ApeiraPayloadType<RealmCreatedPayload> payloadType() {
        return TYPE;
    }
}
