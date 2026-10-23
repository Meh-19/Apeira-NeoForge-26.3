package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.client.BlackTransition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

/** Marks the respawn packet's handling, during which the loading screen opens: see {@link BlackTransition}. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void apeira$startRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        // The packet first arrives on the network thread, which only hands it to the main thread.
        if (Minecraft.getInstance().isSameThread()) {
            BlackTransition.setRespawning(true);
        }
    }

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void apeira$endRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        BlackTransition.setRespawning(false);
    }
}
