package com.apeira.core.network;

import com.apeira.core.network.client.ApeiraClientNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Helpers for sending registered payloads. */
public final class ApeiraPackets {
    private ApeiraPackets() {}

    /**
     * Sends payloads from the client to the server. Client only: calling this on a dedicated server throws, because
     * the client classes it uses are not present there.
     */
    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ApeiraClientNetwork.sendToServer(payload, payloads);
    }

    /** Sends payloads to one player. */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayer(player, payload, payloads);
    }

    /**
     * Sends payloads to every player whose client has the block entity's chunk loaded, which is everyone who can see
     * it. Does nothing when called on the client or for a block entity not yet placed in a level.
     */
    public static void sendToTrackingClients(BlockEntity blockEntity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        if (blockEntity.getLevel() instanceof ServerLevel level) {
            sendToTrackingClients(level, blockEntity.getBlockPos(), payload, payloads);
        }
    }

    /** Sends payloads to every player whose client has the chunk containing {@code pos} loaded. */
    public static void sendToTrackingClients(ServerLevel level, BlockPos pos, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ChunkPos chunk = new ChunkPos(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk, payload, payloads);
    }
}
