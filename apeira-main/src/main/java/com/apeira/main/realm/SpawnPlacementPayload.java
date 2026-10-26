package com.apeira.main.realm;

import java.util.Optional;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: {@code /realm setspawn} (or the realm settings' Set Spawn) starts the 3x3x2 spawn placement cursor
 * ({@code client/realm/SpawnPlacement}); a left-click answers with a {@link ConfirmSpawnPayload}.
 *
 * @param balloon where the realm's starter balloon stands now, which the cursor may overlap
 */
public record SpawnPlacementPayload(Optional<BlockPos> balloon) implements ApeiraPayload<SpawnPlacementPayload> {
    /** How far the cursor reaches, in blocks. */
    public static final double REACH = 32.0;

    public static final ApeiraPayloadType<SpawnPlacementPayload> TYPE = ApeiraPayloadType.of("spawn_placement",
            StreamCodec.composite(ByteBufCodecs.optional(BlockPos.STREAM_CODEC), SpawnPlacementPayload::balloon, SpawnPlacementPayload::new));

    @Override
    public ApeiraPayloadType<SpawnPlacementPayload> payloadType() {
        return TYPE;
    }
}
