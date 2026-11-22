package com.apeira.dev.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.dev.ApeiraDev;
import com.apeira.dev.network.FurnitureTuningPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The furniture tuner's tune log, {@code logs/apeira_furniture_tune_log.json} in the game directory
 * ({@code apeira-main/run/logs/} in dev): every tuned piece's {@link FurnitureTuning#toJson()} keyed by block id, written
 * on every edit (a reset or a tuning back at the code values takes the entry out), for review and for applying to code.
 *
 * <p>Applying moves entries into decor's baked tunes ({@code apeira_decor/furniture_tunes.json}, {@code FurnitureTunes});
 * {@code scripts/apply_furniture_tunes.py} does it and deletes the applied entries. Entries applied by hand go too: on
 * every join, {@link #pruneAndRestore(boolean)} drops each entry the code already matches (so a stale entry is never applied
 * twice) and sends the rest to the server again, so unapplied tunings outlive the session.
 */
public final class TuneLog {
    public static final String FILE = "logs/apeira_furniture_tune_log.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private TuneLog() {}

    public static Path file() {
        return FMLPaths.GAMEDIR.get().resolve(FILE);
    }

    /** Logs {@code block}'s tuning, or drops its entry when the tuning is its code values. */
    static void record(FurnitureBlock block, FurnitureTuning tuning) {
        if (tuning.equals(FurnitureTuning.of(block))) {
            remove(block);
            return;
        }
        JsonObject all = read();
        JsonObject entry = tuning.toJson();
        entry.addProperty("logged_at", Instant.now().toString());
        all.add(BuiltInRegistries.BLOCK.getKey(block).toString(), entry);
        write(all);
    }

    static void remove(FurnitureBlock block) {
        JsonObject all = read();
        if (all.remove(BuiltInRegistries.BLOCK.getKey(block).toString()) != null) {
            write(all);
        }
    }

    /**
     * Drops the entries the code now matches (applied, or for blocks that are gone) and sends the others to the server,
     * which takes them only from creative operators. Returns how many were dropped and how many sent.
     */
    static int[] pruneAndRestore(boolean send) {
        JsonObject all = read();
        List<String> applied = new ArrayList<>();
        int sent = 0;
        for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null || !(BuiltInRegistries.BLOCK.getValue(id) instanceof FurnitureBlock block) || !entry.getValue().isJsonObject()) {
                applied.add(entry.getKey());
                continue;
            }
            FurnitureTuning tuning = FurnitureTuning.fromJson(entry.getValue().getAsJsonObject(), block);
            if (tuning.equals(FurnitureTuning.of(block))) {
                applied.add(entry.getKey());
            } else if (send) {
                ApeiraClientNetwork.sendToServer(new FurnitureTuningPayload(id, Optional.of(tuning), Optional.empty()));
                sent++;
            }
        }
        if (!applied.isEmpty()) {
            applied.forEach(all::remove);
            write(all);
            ApeiraDev.LOGGER.info("Tune log: dropped {} entries the code already has: {}", applied.size(), applied);
        }
        return new int[] {applied.size(), sent};
    }

    private static JsonObject read() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonParseException | IllegalStateException e) {
            ApeiraDev.LOGGER.warn("Could not read {}, starting it again", file, e);
            return new JsonObject();
        }
    }

    private static void write(JsonObject all) {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(all, writer);
            }
        } catch (IOException e) {
            ApeiraDev.LOGGER.warn("Could not write the tune log {}", file, e);
        }
    }
}
