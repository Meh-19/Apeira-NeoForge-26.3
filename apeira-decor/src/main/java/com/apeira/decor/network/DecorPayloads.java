package com.apeira.decor.network;

import com.apeira.core.network.ApeiraNetwork;

/** Payloads of the decor module, passed to {@link com.apeira.decor.ApeiraDecor#NETWORK}. */
public final class DecorPayloads {
    private DecorPayloads() {}

    public static void init(ApeiraNetwork network) {
        network.toServer(OpenWardrobePayload.TYPE, OpenWardrobePayload::handle);
        // Handled in client/ApeiraDecorClient.
        network.toClient(CrateOpenPayload.TYPE);
        network.toClient(CrateUnlockPayload.TYPE);
    }
}
