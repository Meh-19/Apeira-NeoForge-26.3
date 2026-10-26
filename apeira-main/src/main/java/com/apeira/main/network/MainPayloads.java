package com.apeira.main.network;

import com.apeira.core.network.ApeiraNetwork;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.OpenNavigatorPayload;
import com.apeira.main.navigator.OpenProfilePayload;
import com.apeira.main.realm.ConfirmSpawnPayload;
import com.apeira.main.realm.OpenOnboardingPayload;
import com.apeira.main.realm.RealmCreatedPayload;
import com.apeira.main.realm.SelectRealmPresetPayload;
import com.apeira.main.realm.SpawnPlacementPayload;

/** Payloads of the main module, passed to {@link com.apeira.main.Apeira#NETWORK}. */
public final class MainPayloads {
    private MainPayloads() {}

    public static void init(ApeiraNetwork network) {
        network.toServer(RocketTogglePayload.TYPE, RocketTogglePayload::handle);
        network.toServer(NavigatorActionPayload.TYPE, NavigatorActionPayload::handle);
        network.toServer(SelectRealmPresetPayload.TYPE, SelectRealmPresetPayload::handle);
        network.toServer(OpenProfilePayload.TYPE, OpenProfilePayload::handle);
        network.toServer(ConfirmSpawnPayload.TYPE, ConfirmSpawnPayload::handle);
        // Handled in client/ApeiraClient.
        network.toClient(OpenNavigatorPayload.TYPE);
        network.toClient(OpenOnboardingPayload.TYPE);
        network.toClient(RealmCreatedPayload.TYPE);
        network.toClient(SpawnPlacementPayload.TYPE);
    }
}
