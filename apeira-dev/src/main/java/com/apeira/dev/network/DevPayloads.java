package com.apeira.dev.network;

import com.apeira.core.network.ApeiraNetwork;
import com.apeira.dev.TuningSync;

/** Payloads of the dev module, passed to {@link com.apeira.dev.ApeiraDev#NETWORK}. */
public final class DevPayloads {
    private DevPayloads() {}

    public static void init(ApeiraNetwork network) {
        // Clients send edits, the server sends the result to everyone; the client side is handled in client/ApeiraDevClient.
        network.bidirectional(FurnitureTuningPayload.TYPE, TuningSync::handleOnServer);
        network.bidirectional(BackOffsetPayload.TYPE, TuningSync::handleBackOnServer);
    }
}
