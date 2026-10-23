package com.apeira.main.navigator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the player clicked a navigator button. The server checks it against the page it last sent this
 * player and their realm permissions ({@link NavigatorService#handle}), acts, and answers with an
 * {@link OpenNavigatorPayload}.
 *
 * @param argument  the page for {@link Action#OPEN_PAGE}, the step for {@link Action#PAGE}, the rule, role, region,
 *                  dimension or world for the actions that pick one, otherwise 0
 * @param target    the player head clicked, for {@link Action#ENTRY}
 * @param secondary a right-click
 */
public record NavigatorActionPayload(Action action, int argument, Optional<UUID> target, boolean secondary)
        implements ApeiraPayload<NavigatorActionPayload> {
    public static final ApeiraPayloadType<NavigatorActionPayload> TYPE = ApeiraPayloadType.of("navigator_action",
            StreamCodec.composite(Action.STREAM_CODEC, NavigatorActionPayload::action, ByteBufCodecs.VAR_INT, NavigatorActionPayload::argument,
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), NavigatorActionPayload::target, ByteBufCodecs.BOOL,
                    NavigatorActionPayload::secondary, NavigatorActionPayload::new));

    public NavigatorActionPayload(Action action, int argument) {
        this(action, argument, Optional.empty(), false);
    }

    public static NavigatorActionPayload of(Action action) {
        return new NavigatorActionPayload(action, 0);
    }

    public static NavigatorActionPayload open(NavigatorPage page) {
        return new NavigatorActionPayload(Action.OPEN_PAGE, page.ordinal());
    }

    @Override
    public ApeiraPayloadType<NavigatorActionPayload> payloadType() {
        return TYPE;
    }

    public static void handle(NavigatorActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
            NavigatorService.handle(player, payload);
        }
    }

    public enum Action {
        OPEN_PAGE,
        /** Previous (-1) or next (+1) list page. */
        PAGE,
        FILTER,
        SORT,
        SPAWN,
        HOMES,
        /** Teleport to the viewer's own realm. */
        REALM_TELEPORT,
        OPEN_WORLD,
        /** A player head in a list: visit, edit or unban, by page. */
        ENTRY,
        /** Close a popup or modal, back to the page under it. */
        CLOSE_POPUP,
        KICK,
        BAN,
        INVITE,
        /** Give the popup's member a role: {@link com.apeira.main.realm.RealmRole} ordinal. */
        SET_ROLE,
        /** Take the popup's member's membership away. */
        REMOVE_MEMBER,
        VISITING_RULE,
        TOGGLE_CHAT,
        TOGGLE_ACCESS,
        SET_SPAWN,
        /** Open the reset button into its dimension buttons, or pick one: 0 Overworld, 1 Nether, 2 Aether. */
        RESET_DIMENSION,
        RESET_PRESET,
        RESET_CONFIRM;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }
}
