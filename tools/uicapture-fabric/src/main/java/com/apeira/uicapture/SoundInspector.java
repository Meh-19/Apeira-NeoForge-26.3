package com.apeira.uicapture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The sound inspector, a client command for timing sounds such as Origin Realms' Ruby ATM: {@code /apeirasound <sound>
 * [volume] [pitch]} plays a sound event of the loaded resource packs (the server pack included) to the player,
 * unattenuated, at the given volume and pitch (1 by default), and writes to chat and the log the variant it picked: its
 * file, its volume and pitch as played (the {@code sounds.json} entry's times the command's), its attenuation distance
 * (and the reach at that volume), whether it streams, the resource pack the file came from, its length and how long it
 * plays at that pitch (read from the Ogg file), and the event's subtitle key. {@code /apeirasound listen} turns the
 * sound log on or off: while on, every sound the client plays (the server's included) is appended to
 * {@code logs/apeira_sound_log.jsonl} with its time since the log started, so sequences can be timed: event, file,
 * pack, category, volume, pitch, attenuation and distance, position and distance from the player, looping, delay and
 * length. Each logged sound is also written to the game log as one line (event, file, volume, pitch, attenuation
 * distance, subtitle key), and with {@code /apeirasound listen chat} to chat as well. A client command, as only the client knows the resource packs' {@code sounds.json}; nothing is sent to the
 * server.
 */
public final class SoundInspector {
    static final String LOG = "logs/apeira_sound_log.jsonl";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    /** Each sound file's length in seconds, or a negative value when it could not be read. */
    private static final Map<Identifier, Double> LENGTHS = new HashMap<>();

    private static boolean listening;
    private static boolean toChat;
    private static boolean listenerAdded;
    private static long started;
    private static int logged;

    private SoundInspector() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("apeirasound")
                .then(ClientCommandManager.literal("listen")
                        .executes(context -> toggleListening(context, false))
                        .then(ClientCommandManager.literal("chat").executes(context -> toggleListening(context, true))))
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
                "Pack: " + pack(minecraft, sound.getPath()),
                "Length: " + length(minecraft, sound.getPath(), instance.getPitch()),
                "Subtitle: " + subtitle
        };
        for (int i = 0; i < lines.length; i++) {
            context.getSource().sendFeedback(Component.literal(lines[i]).withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            UiCaptureClient.LOGGER.info("[sound inspector] {}", lines[i]);
        }
        return 1;
    }

    private static int toggleListening(CommandContext<FabricClientCommandSource> context, boolean chat) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!listenerAdded) {
            listenerAdded = true;
            minecraft.getSoundManager().addListener(new Listener());
        }
        listening = !listening;
        toChat = listening && chat;
        if (listening) {
            started = System.nanoTime();
            logged = 0;
            JsonObject start = new JsonObject();
            start.addProperty("start", LocalDateTime.now().toString());
            append(start);
            context.getSource().sendFeedback(Component.translatable(chat ? "message.apeira.sound_inspector.listen_chat_on"
                    : "message.apeira.sound_inspector.listen_on", LOG).withStyle(ChatFormatting.GOLD));
        } else {
            context.getSource().sendFeedback(Component.translatable("message.apeira.sound_inspector.listen_off", logged).withStyle(ChatFormatting.GOLD));
        }
        UiCaptureClient.LOGGER.info("[sound inspector] sound log {}", listening ? "on" : "off (" + logged + " sounds)");
        return 1;
    }

    /** Logs every sound the sound engine plays while the sound log is on. */
    private static final class Listener implements SoundEventListener {
        @Override
        public void onPlaySound(SoundInstance instance, WeighedSoundEvents event, float range) {
            if (!listening) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            JsonObject entry = new JsonObject();
            entry.addProperty("ms", (System.nanoTime() - started) / 1_000_000L);
            entry.addProperty("sound", instance.getIdentifier().toString());
            Sound sound = instance.getSound();
            if (sound != null) {
                entry.addProperty("file", sound.getPath().toString());
                entry.addProperty("pack", pack(minecraft, sound.getPath()));
                entry.addProperty("streamed", sound.shouldStream());
                entry.addProperty("attenuation_distance", sound.getAttenuationDistance());
                double seconds = seconds(minecraft, sound.getPath());
                if (seconds >= 0) {
                    entry.addProperty("length_s", round(seconds));
                    entry.addProperty("played_s", round(seconds / Math.max(instance.getPitch(), 0.01F)));
                }
            }
            entry.addProperty("category", instance.getSource().getName());
            entry.addProperty("volume", round(instance.getVolume()));
            entry.addProperty("pitch", round(instance.getPitch()));
            entry.addProperty("attenuation", instance.getAttenuation().name().toLowerCase(Locale.ROOT));
            entry.addProperty("range", round(range));
            entry.addProperty("x", round(instance.getX()));
            entry.addProperty("y", round(instance.getY()));
            entry.addProperty("z", round(instance.getZ()));
            Player player = minecraft.player;
            if (player != null && !instance.isRelative()) {
                entry.addProperty("distance", round(Math.sqrt(player.distanceToSqr(instance.getX(), instance.getY(), instance.getZ()))));
            }
            entry.addProperty("relative", instance.isRelative());
            entry.addProperty("looping", instance.isLooping());
            entry.addProperty("delay", instance.getDelay());
            String subtitle = "-";
            if (event.getSubtitle() != null) {
                subtitle = event.getSubtitle().getContents() instanceof TranslatableContents translatable
                        ? translatable.getKey() : event.getSubtitle().getString();
                entry.addProperty("subtitle", subtitle);
            }
            logged++;
            append(entry);
            String line = instance.getIdentifier() + " | " + (sound == null ? "-" : sound.getPath()) + " | volume " + format(instance.getVolume())
                    + " | pitch " + format(instance.getPitch()) + " | attenuation " + (sound == null ? "-" : sound.getAttenuationDistance())
                    + " (" + instance.getAttenuation().name().toLowerCase(Locale.ROOT) + ") | subtitle " + subtitle;
            UiCaptureClient.LOGGER.info("[sound log] {}", line);
            if (toChat && player != null) {
                player.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.GRAY), false);
            }
        }
    }

    private static void append(JsonObject entry) {
        Path file = FabricLoader.getInstance().getGameDir().resolve(LOG);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(entry) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("Sound log failed", e);
        }
    }

    private static String pack(Minecraft minecraft, Identifier path) {
        return minecraft.getResourceManager().getResource(path).map(Resource::sourcePackId).orElse("-");
    }

    private static String length(Minecraft minecraft, Identifier path, float pitch) {
        double seconds = seconds(minecraft, path);
        if (seconds < 0) {
            return "-";
        }
        return format(seconds) + " s (" + format(seconds / Math.max(pitch, 0.01F)) + " s at this pitch)";
    }

    /** The Ogg Vorbis file's length: its last page's granule position over the identification header's sample rate. */
    private static double seconds(Minecraft minecraft, Identifier path) {
        return LENGTHS.computeIfAbsent(path, file -> {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                return -1.0;
            }
            try (InputStream in = resource.get().open()) {
                return oggSeconds(in.readAllBytes());
            } catch (IOException | RuntimeException e) {
                return -1.0;
            }
        });
    }

    static double oggSeconds(byte[] data) {
        int header = indexOf(data, new byte[] {1, 'v', 'o', 'r', 'b', 'i', 's'}, 0);
        if (header < 0 || header + 16 > data.length) {
            return -1.0;
        }
        long rate = littleEndian(data, header + 12, 4);
        for (int i = data.length - 27; i >= 0; i--) {
            if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g' && data[i + 3] == 'S') {
                long granule = littleEndian(data, i + 6, 8);
                return rate > 0 && granule >= 0 ? (double) granule / rate : -1.0;
            }
        }
        return -1.0;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static long littleEndian(byte[] data, int offset, int bytes) {
        long value = 0;
        for (int i = bytes - 1; i >= 0; i--) {
            value = (value << 8) | (data[offset + i] & 0xFF);
        }
        return value;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
