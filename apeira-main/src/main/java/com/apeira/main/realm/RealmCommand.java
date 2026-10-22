package com.apeira.main.realm;

import com.apeira.core.network.ApeiraPackets;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /realm create} opens the onboarding screen for a player without a realm, {@code /realm home} sends a player
 * to their realm. Any player may use them; the rest of Origin Realms' {@code /realm} commands come with realm
 * settings and members.
 */
public final class RealmCommand {
    private RealmCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("realm")
                .then(Commands.literal("create").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (RealmData.get(player.level().getServer()).realmOf(player.getUUID()).isPresent()) {
                        player.sendSystemMessage(Component.translatable("message.apeira.realm.exists"));
                        return 0;
                    }
                    ApeiraPackets.sendToPlayer(player, new OpenOnboardingPayload());
                    return 1;
                }))
                .then(Commands.literal("home").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!RealmPool.sendHome(player)) {
                        player.sendSystemMessage(Component.translatable("message.apeira.realm.none"));
                        return 0;
                    }
                    return 1;
                })));
    }
}
