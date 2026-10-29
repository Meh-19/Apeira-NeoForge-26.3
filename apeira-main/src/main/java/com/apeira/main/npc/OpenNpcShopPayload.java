package com.apeira.main.npc;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the player right-clicked the NPC with entity id {@code npc} again while its dialogue was open, which
 * opens its shop ({@link NpcService#openShop}, checked there).
 */
public record OpenNpcShopPayload(int npc) implements ApeiraPayload<OpenNpcShopPayload> {
    public static final ApeiraPayloadType<OpenNpcShopPayload> TYPE = ApeiraPayloadType.of("open_npc_shop",
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenNpcShopPayload::npc, OpenNpcShopPayload::new));

    @Override
    public ApeiraPayloadType<OpenNpcShopPayload> payloadType() {
        return TYPE;
    }

    public static void handle(OpenNpcShopPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
            NpcService.openShop(player, payload.npc);
        }
    }
}
