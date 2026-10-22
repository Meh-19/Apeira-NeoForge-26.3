package com.apeira.main.network;

import com.apeira.core.network.ApeiraNetwork;

/** Payloads of the main module, passed to {@link com.apeira.main.Apeira#NETWORK}. */
public final class MainPayloads {
    private MainPayloads() {}

    public static void init(ApeiraNetwork network) {
        network.toServer(RocketTogglePayload.TYPE, RocketTogglePayload::handle);
        network.toClient(OpenNavigatorPayload.TYPE);
        network.toServer(NavigatorActionPayload.TYPE, NavigatorActionPayload::handle);
    }
}
