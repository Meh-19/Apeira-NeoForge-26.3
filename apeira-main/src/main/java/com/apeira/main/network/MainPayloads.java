package com.apeira.main.network;

import com.apeira.core.network.ApeiraNetwork;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.OpenNavigatorPayload;

/** Payloads of the main module, passed to {@link com.apeira.main.Apeira#NETWORK}. */
public final class MainPayloads {
    private MainPayloads() {}

    public static void init(ApeiraNetwork network) {
        network.toServer(RocketTogglePayload.TYPE, RocketTogglePayload::handle);
        network.toServer(NavigatorActionPayload.TYPE, NavigatorActionPayload::handle);
        // Handled in client/ApeiraClient.
        network.toClient(OpenNavigatorPayload.TYPE);
    }
}
