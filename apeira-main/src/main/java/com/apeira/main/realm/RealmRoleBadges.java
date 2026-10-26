package com.apeira.main.realm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Origin Realms' role badges in the player list: a player standing in a realm is listed with the badge of their role
 * there (Ren's {@code textures/gui/role/<role>_playerlist.png}: V grey, R green, T blue, O red, glyphs E000-E003 of the
 * {@code apeira:realm_roles} font, in {@link RealmRole}'s order) before their name. Names are checked twice a second and
 * refreshed when a player's realm or role changed.
 */
public final class RealmRoleBadges {
    private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(ApeiraIds.id("realm_roles")));
    private static final int INTERVAL = 10;
    /** The role each player was last listed with; absent outside realms. */
    private static final Map<UUID, RealmRole> LISTED = new HashMap<>();
    private static int ticks;

    private RealmRoleBadges() {}

    public static Component badge(RealmRole role) {
        return Component.literal(Character.toString(0xE000 + role.ordinal())).withStyle(FONT);
    }

    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RealmService.roleIn(player, player.level()).ifPresent(role -> {
            Component name = event.getDisplayName() != null ? event.getDisplayName() : player.getName();
            event.setDisplayName(Component.empty().append(badge(role)).append(" ").append(name));
        });
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticks % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Optional<RealmRole> role = RealmService.roleIn(player, player.level());
            if (!role.equals(Optional.ofNullable(LISTED.get(player.getUUID())))) {
                role.ifPresentOrElse(value -> LISTED.put(player.getUUID(), value), () -> LISTED.remove(player.getUUID()));
                player.refreshTabListName();
            }
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LISTED.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        LISTED.clear();
    }
}
