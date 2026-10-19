package com.apeira.archae.network;

import com.apeira.core.network.ApeiraNetwork;

/** Payloads of the archae module, passed to {@link com.apeira.archae.ApeiraArchae#NETWORK}. */
public final class ArchaePayloads {
    private ArchaePayloads() {}

    public static void init(ApeiraNetwork network) {
        // Handled in client/ApeiraArchaeClient.
        network.toClient(AssemblyRecipesPayload.TYPE);
    }
}
