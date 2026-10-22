package com.apeira.main.navigator;

import java.util.function.IntFunction;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the player clicked a navigator button. {@code argument} is the page to open for
 * {@link Action#OPEN_PAGE} and the world (0 to 5, red to purple) for {@link Action#OPEN_WORLD}, otherwise 0.
 */
public record NavigatorActionPayload(Action action, int argument) implements ApeiraPayload<NavigatorActionPayload> {
    public static final ApeiraPayloadType<NavigatorActionPayload> TYPE = ApeiraPayloadType.of("navigator_action",
            StreamCodec.composite(Action.STREAM_CODEC, NavigatorActionPayload::action, ByteBufCodecs.VAR_INT,
                    NavigatorActionPayload::argument, NavigatorActionPayload::new));

    public static NavigatorActionPayload open(NavigatorPage page) {
        return new NavigatorActionPayload(Action.OPEN_PAGE, page.ordinal());
    }

    @Override
    public ApeiraPayloadType<NavigatorActionPayload> payloadType() {
        return TYPE;
    }

    public static void handle(NavigatorActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        switch (payload.action) {
            case OPEN_PAGE -> {
                NavigatorPage[] pages = NavigatorPage.values();
                if (payload.argument >= 0 && payload.argument < pages.length) {
                    ApeiraPackets.sendToPlayer(player, new OpenNavigatorPayload(pages[payload.argument]));
                }
            }
            case SPAWN -> teleportToSpawn(player);
            // Realms, homes and open worlds have no backing systems yet; say so instead of doing nothing.
            case REALM_TELEPORT -> unavailable(player, "realm_teleport");
            case HOMES -> unavailable(player, "homes");
            case OPEN_WORLD -> unavailable(player, "open_world");
        }
    }

    /**
     * Sends the player to their spawn point: their bed or anchor if they have one, else the world spawn, without using
     * up an anchor charge. Stands in for the realm spawn until realms exist.
     */
    private static void teleportToSpawn(ServerPlayer player) {
        TeleportTransition transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        player.teleport(transition);
        player.sendSystemMessage(Component.translatable("message.apeira.navigator.spawn"));
    }

    private static void unavailable(ServerPlayer player, String feature) {
        player.sendSystemMessage(Component.translatable("message.apeira.navigator.unavailable",
                Component.translatable("navigator.apeira." + feature)));
    }

    public enum Action {
        OPEN_PAGE,
        SPAWN,
        HOMES,
        REALM_TELEPORT,
        OPEN_WORLD;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }
}
