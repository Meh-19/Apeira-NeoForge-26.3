package com.apeira.main.realm;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

/**
 * Server to client: open the onboarding screen, where a player without a realm picks its region. Sent on joining
 * ({@link RealmPool#onPlayerLoggedIn}) and by {@code /realm create}; handled in {@code client/ApeiraClient}.
 */
public record OpenOnboardingPayload() implements ApeiraPayload<OpenOnboardingPayload> {
    public static final ApeiraPayloadType<OpenOnboardingPayload> TYPE = ApeiraPayloadType.unit("open_onboarding", new OpenOnboardingPayload());

    @Override
    public ApeiraPayloadType<OpenOnboardingPayload> payloadType() {
        return TYPE;
    }
}
