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

import com.apeira.decor.furniture.Furniture;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurniturePlacement;
import com.apeira.decor.furniture.FurnitureTuning;
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
 * keyed by block id so the file collects every exported piece, and prints the Java to the log: the shape as
 * {@code Block.box} calls and the piece's {@code FurnitureCatalogue} line, ready to paste.
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
        String catalogue = catalogueJava(block.furniture(), tuning);
        all.add(id, entry(block.furniture(), tuning, shape, catalogue));
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(all, writer);
        }
        ApeiraDev.LOGGER.info("Furniture export of {} (also in {}):\n{}\n{}", id, file, shape, catalogue);
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

    private static JsonObject entry(Furniture furniture, FurnitureTuning tuning, String shape, String catalogue) {
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
        entry.addProperty("model_bottom", modelBottom(furniture));
        entry.addProperty("rotation", tuning.rotation());
        JsonArray placements = new JsonArray();
        FurniturePlacement.of(tuning.placements()).forEach(placement -> placements.add(placement.name()));
        entry.add("placements", placements);
        if (furniture.seat()) {
            entry.addProperty("seat_height", tuning.seatHeight());
        }
        JsonObject java = new JsonObject();
        java.addProperty("shape", shape);
        java.addProperty("catalogue", catalogue);
        entry.add("java", java);
        entry.addProperty("exported_at", Instant.now().toString());
        return entry;
    }

    /** The shape facing north, as {@code Block.box} calls in pixels. */
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

    /** The piece's {@code FurnitureCatalogue} line, with what the catalogue cannot hold yet as a comment. */
    static String catalogueJava(Furniture furniture, FurnitureTuning tuning) {
        StringBuilder java = new StringBuilder("all.add(piece(\"").append(furniture.name()).append("\", ")
                .append(furniture.crate() == null ? "null" : "\"" + furniture.crate() + "\"").append(")");
        if (furniture.paintable()) {
            java.append(".paintable()");
        }
        if (furniture.defaultPaint() != null) {
            java.append(".defaultPaint(0x").append(String.format("%06X", furniture.defaultPaint())).append(")");
        }
        if (!furniture.collision()) {
            java.append(".noCollision()");
        }
        if (furniture.seat()) {
            java.append(".seat(").append(TuningParameter.number(tuning.seatHeight())).append(")");
        }
        if (furniture.large() != null || tuning.scale() != 1.0F || tuning.offsetY() != 0.0F) {
            java.append(".large(").append(TuningParameter.number(tuning.scale())).append(", ").append(TuningParameter.number(modelBottom(furniture)))
                    .append(", ").append(TuningParameter.number(tuning.offsetY())).append(")");
        }
        java.append(".shape(");
        java.append(tuning.boxes().stream().map(box -> "\n        " + numbers(box)).collect(Collectors.joining(",")));
        java.append("));");
        List<String> extra = new ArrayList<>();
        if (tuning.offsetX() != 0.0F || tuning.offsetZ() != 0.0F) {
            extra.add("offset x " + TuningParameter.number(tuning.offsetX()) + ", z " + TuningParameter.number(tuning.offsetZ()));
        }
        if (tuning.rotation() != 0) {
            extra.add("rotation +" + tuning.rotation() * 45 + " degrees");
        }
        if (tuning.placements() != FurniturePlacement.ANY_FACE) {
            extra.add("placements " + FurniturePlacement.of(tuning.placements()));
        }
        if (furniture.large() == null && (tuning.scale() != 1.0F || tuning.offsetY() != 0.0F)) {
            extra.add("a block-model piece drawn with .large(...) needs its _placed model");
        }
        if (!extra.isEmpty()) {
            java.append("\n// Not in the catalogue yet: ").append(String.join("; ", extra));
        }
        return java.toString();
    }

    private static float modelBottom(Furniture furniture) {
        return furniture.large() != null ? furniture.large().modelBottom() : 0.0F;
    }

    private static String numbers(FurnitureTuning.Box box) {
        return String.join(", ", TuningParameter.number(box.minX()), TuningParameter.number(box.minY()), TuningParameter.number(box.minZ()),
                TuningParameter.number(box.maxX()), TuningParameter.number(box.maxY()), TuningParameter.number(box.maxZ()));
    }
}
