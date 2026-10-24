package com.apeira.uicapture;

import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * The sound inspector, a client command for timing sounds such as Origin Realms' Ruby ATM: {@code /apeirasound <sound>
 * [volume] [pitch]} plays a sound event of the loaded resource packs (the server pack included) to the player,
 * unattenuated, at the given volume and pitch (1 by default), and writes to chat and the log the variant it picked: its
 * file, its volume and pitch as played (the {@code sounds.json} entry's times the command's), its attenuation distance
 * (and the reach at that volume), whether it streams, and the event's subtitle key. A client command, as only the client
 * knows the resource packs' {@code sounds.json}; nothing is sent to the server.
 */
public final class SoundInspector {
    private SoundInspector() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("apeirasound")
                .then(ClientCommandManager.argument("sound", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                Minecraft.getInstance().getSoundManager().getAvailableSounds(), builder))
                        .executes(context -> inspect(context, 1.0F, 1.0F))
                        .then(ClientCommandManager.argument("volume", FloatArgumentType.floatArg(0.0F, 10.0F))
                                .executes(context -> inspect(context, FloatArgumentType.getFloat(context, "volume"), 1.0F))
                                .then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(0.5F, 2.0F))
                                        .executes(context -> inspect(context, FloatArgumentType.getFloat(context, "volume"),
                                                FloatArgumentType.getFloat(context, "pitch")))))));
    }

    private static int inspect(CommandContext<FabricClientCommandSource> context, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier id = context.getArgument("sound", Identifier.class);
        WeighedSoundEvents event = minecraft.getSoundManager().getSoundEvent(id);
        if (event == null) {
            context.getSource().sendError(Component.translatable("message.apeira.sound_inspector.unknown", id.toString()));
            return 0;
        }
        SimpleSoundInstance instance = new SimpleSoundInstance(id, SoundSource.MASTER, volume, pitch,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        // Playing resolves the instance, which picks the variant it plays.
        minecraft.getSoundManager().play(instance);
        Sound sound = instance.getSound();
        if (sound == null) {
            context.getSource().sendError(Component.translatable("message.apeira.sound_inspector.unknown", id.toString()));
            return 0;
        }
        float played = instance.getVolume();
        // The sound engine's reach: the attenuation distance, scaled up by volumes over 1.
        float reach = Math.max(played, 1.0F) * sound.getAttenuationDistance();
        String subtitle = event.getSubtitle() == null ? "-"
                : event.getSubtitle().getContents() instanceof TranslatableContents translatable ? translatable.getKey()
                : event.getSubtitle().getString();
        String[] lines = {
                "Sound: " + id,
                "File: " + sound.getPath() + (sound.shouldStream() ? " (streamed)" : ""),
                "Volume: " + format(played) + " (requested " + format(volume) + ")",
                "Pitch: " + format(instance.getPitch()) + " (requested " + format(pitch) + ")",
                "Attenuation distance: " + sound.getAttenuationDistance() + " (reach " + format(reach) + ")",
                "Subtitle: " + subtitle
        };
        for (int i = 0; i < lines.length; i++) {
            context.getSource().sendFeedback(Component.literal(lines[i]).withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            UiCaptureClient.LOGGER.info("[sound inspector] {}", lines[i]);
        }
        return 1;
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
