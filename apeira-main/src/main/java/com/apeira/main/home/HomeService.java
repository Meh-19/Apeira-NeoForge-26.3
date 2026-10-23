package com.apeira.main.home;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.apeira.main.realm.RealmService;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Setting, using, renaming and deleting homes, for the Homes page ({@code navigator/NavigatorService}) and the
 * {@code /home} commands ({@link HomeCommand}). Everything is checked here on the server: a player only ever touches
 * their own homes, names are checked, and travelling to a home in a realm follows that realm's visiting rules.
 */
public final class HomeService {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1," + HomeData.MAX_NAME_LENGTH + "}");

    private HomeService() {}

    public static List<HomeData.Home> homesOf(ServerPlayer player) {
        return HomeData.get(player.level().getServer()).homesOf(player.getUUID());
    }

    /** Sets a home called {@code name} where {@code player} stands, replacing their home of that name. */
    public static boolean set(ServerPlayer player, String name) {
        if (!NAME.matcher(name).matches()) {
            RealmService.fail(player, "home.bad_name", HomeData.MAX_NAME_LENGTH);
            return false;
        }
        HomeData data = HomeData.get(player.level().getServer());
        if (data.indexOf(player.getUUID(), name) < 0 && data.homesOf(player.getUUID()).size() >= HomeData.MAX_HOMES) {
            RealmService.fail(player, "home.full", HomeData.MAX_HOMES);
            return false;
        }
        data.set(player.getUUID(), new HomeData.Home(name, player.level().dimension().identifier(), player.position(), player.getYRot(),
                player.getXRot(), System.currentTimeMillis()));
        RealmService.done(player, "home.set", name);
        return true;
    }

    /** Sets a home where {@code player} stands under the first free name {@code home1}, {@code home2}, ... */
    public static boolean setNext(ServerPlayer player) {
        HomeData data = HomeData.get(player.level().getServer());
        int number = 1;
        while (data.indexOf(player.getUUID(), "home" + number) >= 0) {
            number++;
        }
        return set(player, "home" + number);
    }

    public static boolean teleport(ServerPlayer player, int index) {
        Optional<HomeData.Home> home = home(player, index);
        if (home.isEmpty()) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel level = server.getLevel(home.get().levelKey());
        if (level == null) {
            RealmService.fail(player, "home.missing_world", home.get().name());
            return false;
        }
        Optional<UUID> owner = RealmService.ownerOf(level);
        if (owner.isPresent() && !RealmService.canVisit(server, owner.get(), player)) {
            RealmService.fail(player, "realm.cannot_visit", RealmService.data(server).nameOf(owner.get()));
            return false;
        }
        player.teleport(new TeleportTransition(level, home.get().position(), Vec3.ZERO, home.get().yRot(), home.get().xRot(),
                TeleportTransition.DO_NOTHING));
        RealmService.done(player, "home.teleported", home.get().name());
        return true;
    }

    public static boolean delete(ServerPlayer player, int index) {
        Optional<HomeData.Home> home = home(player, index);
        home.ifPresent(found -> {
            HomeData.get(player.level().getServer()).remove(player.getUUID(), index);
            RealmService.done(player, "home.deleted", found.name());
        });
        return home.isPresent();
    }

    public static boolean rename(ServerPlayer player, int index, String name) {
        Optional<HomeData.Home> home = home(player, index);
        if (home.isEmpty()) {
            return false;
        }
        if (!NAME.matcher(name).matches()) {
            RealmService.fail(player, "home.bad_name", HomeData.MAX_NAME_LENGTH);
            return false;
        }
        HomeData data = HomeData.get(player.level().getServer());
        int existing = data.indexOf(player.getUUID(), name);
        if (existing >= 0 && existing != index) {
            RealmService.fail(player, "home.taken", name);
            return false;
        }
        data.rename(player.getUUID(), index, name);
        RealmService.done(player, "home.renamed", home.get().name(), name);
        return true;
    }

    /** {@code player}'s home at {@code index}, or empty after telling them it is gone. */
    private static Optional<HomeData.Home> home(ServerPlayer player, int index) {
        List<HomeData.Home> homes = homesOf(player);
        if (index < 0 || index >= homes.size()) {
            RealmService.fail(player, "home.unknown");
            return Optional.empty();
        }
        return Optional.of(homes.get(index));
    }

    /** "Jitter_'s Realm", "Overworld", ...: the World line of a home's tooltip. */
    public static Component worldName(MinecraftServer server, HomeData.Home home) {
        Optional<UUID> owner = RealmService.data(server).ownerOf(home.dimension());
        if (owner.isPresent()) {
            return Component.translatable("navigator.apeira.home.realm_world", RealmService.data(server).nameOf(owner.get()));
        }
        if (home.levelKey() == Level.OVERWORLD) {
            return Component.translatable("navigator.apeira.dimension.overworld");
        }
        if (home.levelKey() == Level.NETHER) {
            return Component.translatable("navigator.apeira.dimension.nether");
        }
        if (home.levelKey() == Level.END) {
            return Component.translatable("navigator.apeira.dimension.end");
        }
        return Component.literal(home.dimension().getPath());
    }

    /** The head shown for a home: the realm owner's in a realm, otherwise the player's own. */
    public static UUID icon(MinecraftServer server, HomeData.Home home, UUID player) {
        return RealmService.data(server).ownerOf(home.dimension()).orElse(player);
    }
}
