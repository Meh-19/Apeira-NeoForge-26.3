package com.apeira.main.npc;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: the NPC settings' Set Anchor starts the anchor placement cursor ({@code client/npc/NpcAnchorPlacement})
 * for the NPC with entity id {@code npc}; a left-click answers with a {@link ConfirmNpcAnchorPayload}.
 */
public record NpcAnchorPayload(int npc) implements ApeiraPayload<NpcAnchorPayload> {
    /** How far the cursor reaches, in blocks. */
    public static final double REACH = 32.0;

    public static final ApeiraPayloadType<NpcAnchorPayload> TYPE = ApeiraPayloadType.of("npc_anchor",
            StreamCodec.composite(ByteBufCodecs.VAR_INT, NpcAnchorPayload::npc, NpcAnchorPayload::new));

    @Override
    public ApeiraPayloadType<NpcAnchorPayload> payloadType() {
        return TYPE;
    }
}
