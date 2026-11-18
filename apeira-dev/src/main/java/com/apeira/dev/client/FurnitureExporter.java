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
import java.util.stream.Collectors;

import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurniturePlacement;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.SeatFurnitureBlock;
import com.apeira.dev.ApeiraDev;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Writes a piece's tuning to {@code logs/apeira_furniture_export.json} in the game directory ({@code run/} in dev),
 * keyed by block id so the file collects every exported piece, and prints to the log the shape as {@code Block.box}
 * calls and the piece's footprint table line ({@code name|straight||}, pixel boxes at turn 0), ready to paste into its
 * module's footprint table; the model's scale, offset and turn go into its item model's display.
 */
public final class FurnitureExporter {
    public static final String FILE = "logs/apeira_furniture_export.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private FurnitureExporter() {}

    /** Exports {@code block}'s tuning and returns the file written. */
    public static Path export(FurnitureBlock block, FurnitureTuning tuning) throws IOException {
        Path file = FMLPaths.GAMEDIR.get().resolve(FILE);
        JsonObject all = read(file);
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        String shape = shapeJava(tuning);
        String footprint = footprintLine(BuiltInRegistries.BLOCK.getKey(block).getPath(), tuning);
        all.add(id, entry(block, tuning, shape, footprint));
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(all, writer);
        }
        ApeiraDev.LOGGER.info("Furniture export of {} (also in {}):\n{}\n{}", id, file, shape, footprint);
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

    private static JsonObject entry(FurnitureBlock block, FurnitureTuning tuning, String shape, String footprint) {
        JsonObject entry = new JsonObject();
        JsonArray boxes = new JsonArray();
        for (FurnitureTuning.Box box : tuning.boxes()) {
            JsonArray numbers = new JsonArray();
            for (double value : new double[] {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}) {
                numbers.add(value);
            }
            boxes.add(numbers);
        }
        entry.add("boxes", boxes);
        entry.addProperty("scale", tuning.scale());
        JsonArray offset = new JsonArray();
        offset.add(tuning.offsetX());
        offset.add(tuning.offsetY());
        offset.add(tuning.offsetZ());
        entry.add("offset", offset);
        entry.addProperty("rotation", tuning.rotation());
        JsonArray placements = new JsonArray();
        FurniturePlacement.of(tuning.placements()).forEach(placement -> placements.add(placement.name()));
        entry.add("placements", placements);
        if (block instanceof SeatFurnitureBlock) {
            entry.addProperty("seat_height", tuning.seatHeight());
        }
        JsonObject java = new JsonObject();
        java.addProperty("shape", shape);
        java.addProperty("footprint", footprint);
        entry.add("java", java);
        entry.addProperty("exported_at", Instant.now().toString());
        return entry;
    }

    /** The shape at turn 0, as {@code Block.box} calls in pixels. */
    static String shapeJava(FurnitureTuning tuning) {
        if (tuning.boxes().isEmpty()) {
            return "Shapes.empty()";
        }
        List<String> boxes = new ArrayList<>();
        for (FurnitureTuning.Box box : tuning.boxes()) {
            boxes.add("Block.box(" + numbers(box) + ")");
        }
        return boxes.size() == 1 ? boxes.getFirst() : "Shapes.or(\n        " + String.join(",\n        ", boxes) + ")";
    }

    /** The piece's footprint table line: its boxes at turn 0, the diagonal and outline left for the generator. */
    static String footprintLine(String name, FurnitureTuning tuning) {
        String boxes = tuning.boxes().stream().map(box -> numbers(box).replace(", ", " ")).collect(Collectors.joining(","));
        return name + "|" + boxes + "||";
    }

    private static String numbers(FurnitureTuning.Box box) {
        return String.join(", ", TuningParameter.number(box.minX()), TuningParameter.number(box.minY()), TuningParameter.number(box.minZ()),
                TuningParameter.number(box.maxX()), TuningParameter.number(box.maxY()), TuningParameter.number(box.maxZ()));
    }
}
