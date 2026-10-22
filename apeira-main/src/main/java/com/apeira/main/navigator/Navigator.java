package com.apeira.main.navigator;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.main.network.NavigatorActionPayload;
import com.apeira.main.network.OpenNavigatorPayload;
import com.mojang.brigadier.Command;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Server side of the navigator: the {@code /navigator} command and the buttons' actions. The server decides every page
 * change and sends it back with {@link OpenNavigatorPayload}, as Origin Realms' server menus did.
 */
public final class Navigator {
    /** Origin Realms' six open worlds, in order. */
    public static final String[] OPEN_WORLDS = {"red", "orange", "yellow", "green", "blue", "purple"};

    private Navigator() {}

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("navigator").executes(context -> {
            open(context.getSource().getPlayerOrException(), NavigatorPage.MAIN);
            return Command.SINGLE_SUCCESS;
        }));
    }

    public static void open(ServerPlayer player, NavigatorPage page) {
        ApeiraPackets.sendToPlayer(player, new OpenNavigatorPayload(page));
    }

    public static void handle(ServerPlayer player, NavigatorActionPayload payload) {
        NavigatorAction action = payload.action();
        NavigatorPage page = action.page();
        if (page != null) {
            open(player, page);
            return;
        }
        switch (action) {
            case SPAWN -> {
                // Realms are not added yet, so a player's realm spawn is their respawn point.
                player.teleport(player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
                player.sendSystemMessage(Component.translatable("message.apeira.navigator.spawn"));
            }
            case HOMES -> player.sendSystemMessage(Component.translatable("message.apeira.navigator.homes_unavailable"));
            case OPEN_WORLD -> {
                if (payload.index() >= 0 && payload.index() < OPEN_WORLDS.length) {
                    player.sendSystemMessage(Component.translatable("message.apeira.navigator.open_world_unavailable",
                            Component.translatable("navigator.apeira.open_world." + OPEN_WORLDS[payload.index()])));
                }
            }
            default -> {}
        }
    }
}
