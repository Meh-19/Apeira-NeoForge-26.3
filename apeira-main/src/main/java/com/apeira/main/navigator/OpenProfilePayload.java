package com.apeira.main.navigator;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.main.realm.DummyPlayers;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the profile key was pressed, aiming at entity {@code entityId} (-1 for none). The server opens the
 * profile of the player (or dummy player's mannequin) aimed at when it is near, else the presser's own.
 */
public record OpenProfilePayload(int entityId) implements ApeiraPayload<OpenProfilePayload> {
    public static final ApeiraPayloadType<OpenProfilePayload> TYPE = ApeiraPayloadType.of("open_profile",
            ByteBufCodecs.VAR_INT.map(OpenProfilePayload::new, OpenProfilePayload::entityId));
    /** How far away a player may be aimed at, in blocks. */
    private static final double REACH = 16.0;

    @Override
    public ApeiraPayloadType<OpenProfilePayload> payloadType() {
        return TYPE;
    }

    public static void handle(OpenProfilePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
            Entity entity = payload.entityId < 0 ? null : player.level().getEntity(payload.entityId);
            NavigatorService.openProfile(player, entity != null && entity.distanceTo(player) <= REACH ? profileOf(entity) : null);
        }
    }

    /** Whose profile {@code entity} shows: a player's, a dummy player's mannequin's, or nobody's. */
    public static @Nullable UUID profileOf(Entity entity) {
        if (entity instanceof Player other) {
            return other.getUUID();
        }
        return DummyPlayers.ofEntity(entity).map(DummyPlayers.Dummy::id).orElse(null);
    }
}
