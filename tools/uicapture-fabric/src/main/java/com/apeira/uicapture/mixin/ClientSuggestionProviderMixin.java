package com.apeira.uicapture.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.uicapture.CommandLogger;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;

/**
 * Hands the server's command completions to the {@link CommandLogger}: plugin commands often reach the client as one
 * greedy argument that asks the server ({@code minecraft:ask_server}), so their subcommands exist only in these answers.
 * The chat box's own requests are recorded as they come back; answers to the logger's probes are taken out, so the chat
 * box never sees them.
 */
@Mixin(ClientSuggestionProvider.class)
public abstract class ClientSuggestionProviderMixin {
    @Shadow
    private int pendingSuggestionsId;

    @Inject(method = "customSuggestion", at = @At("HEAD"))
    private void apeira$noteRequest(CommandContext<?> context, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        // The method sends the request under the next id.
        CommandLogger.requested(this.pendingSuggestionsId + 1, context.getInput());
    }

    @Inject(method = "completeCustomSuggestions", at = @At("HEAD"), cancellable = true)
    private void apeira$recordAnswer(int id, Suggestions suggestions, CallbackInfo ci) {
        if (CommandLogger.answered(id, suggestions)) {
            ci.cancel();
        }
    }
}
