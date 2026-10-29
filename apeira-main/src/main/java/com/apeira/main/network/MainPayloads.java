package com.apeira.main.network;

import com.apeira.core.network.ApeiraNetwork;
import com.apeira.main.navigator.NavigatorActionPayload;
import com.apeira.main.navigator.OpenNavigatorPayload;
import com.apeira.main.navigator.OpenProfilePayload;
import com.apeira.main.npc.ConfirmNpcAnchorPayload;
import com.apeira.main.npc.NpcAnchorPayload;
import com.apeira.main.npc.NpcDialoguePayload;
import com.apeira.main.npc.OpenNpcShopPayload;
import com.apeira.main.realm.ConfirmSpawnPayload;
import com.apeira.main.realm.OpenOnboardingPayload;
import com.apeira.main.realm.RealmCreatedPayload;
import com.apeira.main.realm.SelectRealmPresetPayload;
import com.apeira.main.realm.RealmHudPayload;
import com.apeira.main.realm.SpawnPlacementPayload;
import com.apeira.main.ruby.RubyChangePayload;

/** Payloads of the main module, passed to {@link com.apeira.main.Apeira#NETWORK}. */
public final class MainPayloads {
    private MainPayloads() {}

    public static void init(ApeiraNetwork network) {
        network.toServer(RocketTogglePayload.TYPE, RocketTogglePayload::handle);
        network.toServer(NavigatorActionPayload.TYPE, NavigatorActionPayload::handle);
        network.toServer(SelectRealmPresetPayload.TYPE, SelectRealmPresetPayload::handle);
        network.toServer(OpenProfilePayload.TYPE, OpenProfilePayload::handle);
        network.toServer(ConfirmSpawnPayload.TYPE, ConfirmSpawnPayload::handle);
        network.toServer(ConfirmNpcAnchorPayload.TYPE, ConfirmNpcAnchorPayload::handle);
        network.toServer(OpenNpcShopPayload.TYPE, OpenNpcShopPayload::handle);
        // Handled in client/ApeiraClient.
        network.toClient(OpenNavigatorPayload.TYPE);
        network.toClient(OpenOnboardingPayload.TYPE);
        network.toClient(RealmCreatedPayload.TYPE);
        network.toClient(SpawnPlacementPayload.TYPE);
        network.toClient(NpcAnchorPayload.TYPE);
        network.toClient(NpcDialoguePayload.TYPE);
        network.toClient(RealmHudPayload.TYPE);
        network.toClient(RubyChangePayload.TYPE);
    }
}
