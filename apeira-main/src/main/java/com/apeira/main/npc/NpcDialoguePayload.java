package com.apeira.main.npc;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server to client: the player right-clicked the NPC with entity id {@code npc}, which says the line with translation key
 * {@code line}; the client opens the dialogue box ({@code client/npc/NpcDialogueScreen}) and turns the camera to the NPC.
 * {@code shop} says whether the NPC has a shop, which left-clicking it in the dialogue opens ({@link OpenNpcShopPayload}).
 */
public record NpcDialoguePayload(int npc, String line, boolean shop) implements ApeiraPayload<NpcDialoguePayload> {
    public static final ApeiraPayloadType<NpcDialoguePayload> TYPE = ApeiraPayloadType.of("npc_dialogue",
            StreamCodec.composite(ByteBufCodecs.VAR_INT, NpcDialoguePayload::npc, ByteBufCodecs.STRING_UTF8, NpcDialoguePayload::line,
                    ByteBufCodecs.BOOL, NpcDialoguePayload::shop, NpcDialoguePayload::new));

    @Override
    public ApeiraPayloadType<NpcDialoguePayload> payloadType() {
        return TYPE;
    }
}
