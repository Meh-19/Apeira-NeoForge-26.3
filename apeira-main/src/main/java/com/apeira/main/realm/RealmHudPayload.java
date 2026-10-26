package com.apeira.main.realm;

import java.util.Optional;
import java.util.UUID;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server to client: the realm the player is in, for the top bar HUD ({@code client/realm/RealmTopBar}): its owner's
 * name, empty outside realms, where its starter balloon stands (empty when the spawn was set without it) and the
 * player's {@link RealmRole} there (its ordinal, -1 outside realms). Sent on login, on every change of dimension and
 * respawn, to everyone in a realm when its balloon moves ({@link #syncLevel}), and when the player's role changes
 * ({@link RealmRoleBadges}).
 */
public record RealmHudPayload(Optional<String> owner, Optional<BlockPos> balloon, int role) implements ApeiraPayload<RealmHudPayload> {
    public static final ApeiraPayloadType<RealmHudPayload> TYPE = ApeiraPayloadType.of("realm_hud", StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(16)), RealmHudPayload::owner,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), RealmHudPayload::balloon,
            ByteBufCodecs.VAR_INT, RealmHudPayload::role,
            RealmHudPayload::new));

    @Override
    public ApeiraPayloadType<RealmHudPayload> payloadType() {
        return TYPE;
    }

    /** Tells {@code player} about the realm they are in now. */
    public static void sync(ServerPlayer player) {
        ApeiraPackets.sendToPlayer(player, of(player.level(), player));
    }

    /** Tells everyone in {@code level} about it again, after its balloon moved. */
    public static void syncLevel(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            sync(player);
        }
    }

    private static RealmHudPayload of(ServerLevel level, ServerPlayer player) {
        Optional<UUID> owner = RealmService.ownerOf(level);
        if (owner.isEmpty()) {
            return new RealmHudPayload(Optional.empty(), Optional.empty(), -1);
        }
        RealmData data = RealmData.get(level.getServer());
        RealmAccess access = data.access(owner.get());
        return new RealmHudPayload(Optional.of(data.nameOf(owner.get())), access.balloon(), access.roleOf(owner.get(), player.getUUID()).ordinal());
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
