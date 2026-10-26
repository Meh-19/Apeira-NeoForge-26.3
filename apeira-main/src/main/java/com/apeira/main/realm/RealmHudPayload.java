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
 * name, empty outside realms, and where its starter balloon stands. Sent on login, on every change of dimension and
 * respawn, and to everyone in a realm when its balloon moves ({@link #syncLevel}).
 */
public record RealmHudPayload(Optional<String> owner, Optional<BlockPos> balloon) implements ApeiraPayload<RealmHudPayload> {
    public static final ApeiraPayloadType<RealmHudPayload> TYPE = ApeiraPayloadType.of("realm_hud", StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(16)), RealmHudPayload::owner,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), RealmHudPayload::balloon,
            RealmHudPayload::new));

    @Override
    public ApeiraPayloadType<RealmHudPayload> payloadType() {
        return TYPE;
    }

    /** Tells {@code player} about the realm they are in now. */
    public static void sync(ServerPlayer player) {
        ApeiraPackets.sendToPlayer(player, of(player.level()));
    }

    /** Tells everyone in {@code level} about it again, after its balloon moved. */
    public static void syncLevel(ServerLevel level) {
        RealmHudPayload payload = of(level);
        for (ServerPlayer player : level.players()) {
            ApeiraPackets.sendToPlayer(player, payload);
        }
    }

    private static RealmHudPayload of(ServerLevel level) {
        Optional<UUID> owner = RealmService.ownerOf(level);
        if (owner.isEmpty()) {
            return new RealmHudPayload(Optional.empty(), Optional.empty());
        }
        RealmData data = RealmData.get(level.getServer());
        return new RealmHudPayload(Optional.of(data.nameOf(owner.get())), data.access(owner.get()).balloon());
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
