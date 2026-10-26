package com.apeira.main.realm;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the owner left-clicked a clear spot with the spawn placement cursor ({@link SpawnPlacementPayload}).
 * The server checks it again and moves the starter balloon and the spawn there ({@link RealmService#moveSpawn}).
 *
 * @param balloon the centre of the balloon's floor, the block above the clicked surface
 */
public record ConfirmSpawnPayload(BlockPos balloon) implements ApeiraPayload<ConfirmSpawnPayload> {
    public static final ApeiraPayloadType<ConfirmSpawnPayload> TYPE = ApeiraPayloadType.of("confirm_spawn",
            StreamCodec.composite(BlockPos.STREAM_CODEC, ConfirmSpawnPayload::balloon, ConfirmSpawnPayload::new));

    @Override
    public ApeiraPayloadType<ConfirmSpawnPayload> payloadType() {
        return TYPE;
    }

    public static void handle(ConfirmSpawnPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
            RealmService.moveSpawn(player, payload.balloon);
        }
    }
}
