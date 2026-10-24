package com.apeira.dev.client;

import com.apeira.dev.ApeiraDev;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * The sound inspector, a client command for timing sounds such as the Ruby ATM's: {@code /apeirasound <sound> [volume]
 * [pitch]} plays a sound event of the loaded resource packs to the player (unattenuated, at the given volume and pitch,
 * 1 by default) and writes to chat and the log the variant it picked: its file, its volume and pitch as played (the
 * {@code sounds.json} entry's times the command's), its attenuation distance (and the reach at that volume), whether it
 * streams, and the event's subtitle key. A client command, as only the client knows
 * the resource packs' {@code sounds.json}.
 */
public final class SoundInspector {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SoundInspector() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("apeirasound")
                .then(Commands.argument("sound", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                Minecraft.getInstance().getSoundManager().getAvailableSounds(), builder))
                        .executes(context -> inspect(context, 1.0F, 1.0F))
                        .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0F, 10.0F))
                                .executes(context -> inspect(context, FloatArgumentType.getFloat(context, "volume"), 1.0F))
                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5F, 2.0F))
                                        .executes(context -> inspect(context, FloatArgumentType.getFloat(context, "volume"),
                                                FloatArgumentType.getFloat(context, "pitch")))))));
    }

    private static int inspect(CommandContext<CommandSourceStack> context, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier id = IdentifierArgument.getId(context, "sound");
        WeighedSoundEvents event = minecraft.getSoundManager().getSoundEvent(id);
        if (event == null) {
            say(Component.translatable("message.apeira.dev.sound.unknown", id.toString()).withStyle(ChatFormatting.RED));
            return 0;
        }
        SimpleSoundInstance instance = new SimpleSoundInstance(id, SoundSource.MASTER, volume, pitch,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        // Playing resolves the instance, which picks the variant it plays.
        minecraft.getSoundManager().play(instance);
        Sound sound = instance.getSound();
        if (sound == null) {
            say(Component.translatable("message.apeira.dev.sound.unknown", id.toString()).withStyle(ChatFormatting.RED));
            return 0;
        }
        float played = instance.getVolume();
        String subtitle = event.getSubtitle() == null ? "-"
                : event.getSubtitle().getContents() instanceof TranslatableContents translatable ? translatable.getKey()
                : event.getSubtitle().getString();
        String[] lines = {
                "Sound: " + id,
                "File: " + sound.getPath() + (sound.shouldStream() ? " (streamed)" : ""),
                "Volume: " + format(played) + " (requested " + format(volume) + ")",
                "Pitch: " + format(instance.getPitch()) + " (requested " + format(pitch) + ")",
                "Attenuation distance: " + sound.getAttenuationDistance() + " (reach " + format(sound.getAttenuationDistance(played)) + ")",
                "Subtitle: " + subtitle
        };
        for (int i = 0; i < lines.length; i++) {
            say(Component.literal(lines[i]).withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            LOGGER.info("[{} sound inspector] {}", ApeiraDev.MOD_ID, lines[i]);
        }
        return 1;
    }

    private static void say(Component line) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(line);
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
