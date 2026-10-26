package com.apeira.dev.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.apeira.decor.cosmetic.BackOffset;
import com.apeira.dev.ApeiraDev;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Writes a back cosmetic's offset to {@code logs/apeira_backswag_export.json} in the game directory, keyed by item id so
 * the file collects every exported cosmetic, and prints it to the log.
 */
public final class BackswagExporter {
    public static final String FILE = "logs/apeira_backswag_export.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private BackswagExporter() {}

    public static Path export(Item item, BackOffset offset) throws IOException {
        Path file = FMLPaths.GAMEDIR.get().resolve(FILE);
        JsonObject all = read(file);
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        JsonObject entry = new JsonObject();
        entry.addProperty("x", offset.x());
        entry.addProperty("y", offset.y());
        entry.addProperty("z", offset.z());
        entry.addProperty("pitch", offset.pitch());
        entry.addProperty("yaw", offset.yaw());
        entry.addProperty("roll", offset.roll());
        entry.addProperty("scale", offset.scale());
        entry.addProperty("exported", Instant.now().toString());
        all.add(id, entry);
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(all, writer);
        }
        ApeiraDev.LOGGER.info("Back cosmetic export of {} (also in {}): {}", id, file, offset);
        return file;
    }

    private static JsonObject read(Path file) {
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
}
