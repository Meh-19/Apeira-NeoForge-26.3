package com.apeira.main.client.npc;

import com.apeira.main.client.realm.RealmTopBar;
import com.apeira.main.npc.NpcEntity;
import com.apeira.main.realm.RealmRole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.Permissions;

/** The client's guess at {@link NpcEntity#canEdit}, which decides whether a hidden NPC shows as a ghost and can be aimed at. */
public final class NpcClient {
    private NpcClient() {}

    /** Hooks {@link #canEdit} into {@link NpcEntity#clientCanEdit}. */
    public static void init() {
        NpcEntity.clientCanEdit = NpcClient::canEdit;
    }

    /**
     * Whether the client's player may edit {@code npc}: the owner of the realm they are in ({@link RealmTopBar#role}, which
     * the server syncs), or an operator in creative or spectator mode. The server checks again.
     */
    public static boolean canEdit(NpcEntity npc) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if ((player.isCreative() || player.isSpectator()) && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        return RealmTopBar.role() == RealmRole.OWNER.ordinal();
    }
}
