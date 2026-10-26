package com.apeira.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.cosmetic.BackOffset;
import com.apeira.decor.cosmetic.BackOffsets;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTunings;
import com.apeira.dev.network.BackOffsetPayload;
import com.apeira.dev.network.FurnitureTuningPayload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Keeps every client's {@link FurnitureTunings} and {@link BackOffsets} in step with the server's. Edits come from
 * clients as {@link FurnitureTuningPayload}s and {@link BackOffsetPayload}s, are kept in range and sent on to everyone; players who join get every tuning. Tunings
 * live only in memory: they end with the server, and the exporter is how they are kept.
 */
public final class TuningSync {
    private TuningSync() {}

    /** A client changed or reset a piece. Only creative operators may, as with command blocks. */
    public static void handleOnServer(FurnitureTuningPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(BuiltInRegistries.BLOCK.getValue(payload.block()) instanceof FurnitureBlock block)) {
            return;
        }
        if (!player.canUseGameMasterBlocks()) {
            player.sendSystemMessage(Component.translatable("message.apeira.dev.no_permission"));
            // Undo the client's own copy of the edit.
            PacketDistributor.sendToPlayer(player, payload(block, FurnitureTunings.get(block)));
            return;
        }
        FurnitureTuning tuning = payload.tuning().map(FurnitureTuning::sanitized).orElse(null);
        FurnitureTunings.set(block, tuning);
        PacketDistributor.sendToAllPlayers(payload(block, tuning));
    }

    /** A client moved or reset a back cosmetic. Only creative operators may. */
    public static void handleBackOnServer(BackOffsetPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getValue(payload.item());
        if (!(item instanceof CosmeticItem cosmetic) || cosmetic.slot() != CosmeticSlot.BACK) {
            return;
        }
        if (!player.canUseGameMasterBlocks()) {
            player.sendSystemMessage(Component.translatable("message.apeira.dev.no_permission"));
            PacketDistributor.sendToPlayer(player, backPayload(item, BackOffsets.get(item)));
            return;
        }
        BackOffset offset = payload.offset().map(BackOffset::sanitized).orElse(null);
        BackOffsets.set(item, offset);
        PacketDistributor.sendToAllPlayers(backPayload(item, offset));
    }

    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<CustomPacketPayload> payloads = new ArrayList<>();
        FurnitureTunings.all().forEach((block, tuning) -> payloads.add(payload(block, tuning)));
        BackOffsets.all().forEach((item, offset) -> payloads.add(backPayload(item, offset)));
        if (!payloads.isEmpty()) {
            PacketDistributor.sendToPlayer(player, payloads.getFirst(),
                    payloads.subList(1, payloads.size()).toArray(CustomPacketPayload[]::new));
        }
    }

    static void onServerStopped(ServerStoppedEvent event) {
        FurnitureTunings.clear();
        BackOffsets.clear();
    }

    private static FurnitureTuningPayload payload(Block block, FurnitureTuning tuning) {
        return new FurnitureTuningPayload(BuiltInRegistries.BLOCK.getKey(block), Optional.ofNullable(tuning));
    }

    private static BackOffsetPayload backPayload(Item item, @Nullable BackOffset offset) {
        return new BackOffsetPayload(BuiltInRegistries.ITEM.getKey(item), Optional.ofNullable(offset));
    }
}
