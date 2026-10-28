package com.apeira.main.npc;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the player left-clicked a clear spot with the NPC anchor cursor ({@link NpcAnchorPayload}). The
 * server checks it again and moves the NPC there, facing the player ({@link NpcService#anchor}).
 *
 * @param npc    the NPC's entity id
 * @param anchor the block the NPC will stand in, above the clicked surface
 */
public record ConfirmNpcAnchorPayload(int npc, BlockPos anchor) implements ApeiraPayload<ConfirmNpcAnchorPayload> {
    public static final ApeiraPayloadType<ConfirmNpcAnchorPayload> TYPE = ApeiraPayloadType.of("confirm_npc_anchor",
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ConfirmNpcAnchorPayload::npc, BlockPos.STREAM_CODEC, ConfirmNpcAnchorPayload::anchor,
                    ConfirmNpcAnchorPayload::new));

    @Override
    public ApeiraPayloadType<ConfirmNpcAnchorPayload> payloadType() {
        return TYPE;
    }

    public static void handle(ConfirmNpcAnchorPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
            NpcService.anchor(player, payload.npc, payload.anchor);
        }
    }
}
