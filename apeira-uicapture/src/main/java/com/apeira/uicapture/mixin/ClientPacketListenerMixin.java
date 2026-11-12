package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.apeira.uicapture.CommandLogger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Hands every command the player sends to the server to the command logger, as Fabric's
 * {@code ClientSendMessageEvents.COMMAND} did: {@code sendCommand} first offers the command to NeoForge's client
 * commands, and only a command they did not run (unknown to them, so it goes on to the server) is logged.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @ModifyExpressionValue(method = "sendCommand(Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientCommandHandler;runCommand(Ljava/lang/String;)Z"))
    private boolean apeira$logCommand(boolean ranOnClient, String command) {
        if (!ranOnClient) {
            CommandLogger.ran(command);
        }
        return ranOnClient;
    }
}
