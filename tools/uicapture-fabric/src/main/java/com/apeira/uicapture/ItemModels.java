package com.apeira.uicapture;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Resolves a recorded item (the JSON of {@link ArmorStandRecorder#item}: id, item model, custom model data) to the model
 * JSON the client draws, read from the loaded resource packs, the server pack included: the item definition
 * ({@code assets/<ns>/items/<path>.json}) is evaluated for the item's custom model data and display context
 * ({@code model}, {@code range_dispatch}, {@code select}, {@code condition} and {@code composite}; other properties take
 * their fallback, {@code on_false} or 0), then each model's {@code parent} chain is followed, merging {@code textures} and
 * {@code display} and taking the nearest {@code elements}, and texture variables ({@code #layer0}) are resolved down to
 * texture ids. Models without elements whose chain ends in {@code builtin/generated} are generated items (their
 * {@code layer<n>} textures). Every file read is copied into the export folder through {@link CapturedAssets}.
 */
final class ItemModels {
    /** A resolved model: its parent chain (itself first), texture variables resolved to texture ids, elements, merged display transforms. */
    record Model(Identifier id, List<Identifier> chain, Map<String, Identifier> textures, @Nullable JsonArray elements, JsonObject display,
            boolean generated) {
    }

    /** What an item draws in one display context: the definition read, its models, and why something could not be drawn. */
    record Resolved(Identifier definition, List<Model> models, List<String> notes) {
    }

    private final ResourceManager resources;
    private final CapturedAssets assets;
    private final Map<Identifier, Optional<JsonObject>> files = new LinkedHashMap<>();

    ItemModels(ResourceManager resources, CapturedAssets assets) {
        this.resources = resources;
        this.assets = assets;
    }

    Resolved resolve(JsonObject item, String context) {
        Identifier definition = Identifier.parse(item.has("item_model") ? item.get("item_model").getAsString() : item.get("id").getAsString());
        List<Model> models = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Identifier file = definition.withPath(path -> "items/" + path + ".json");
        Optional<JsonObject> json = read(file);
        if (json.isEmpty() || !json.get().has("model")) {
            notes.add("no item definition " + file);
        } else {
            JsonObject cmd = item.has("custom_model_data") ? item.getAsJsonObject("custom_model_data") : new JsonObject();
            pick(json.get().getAsJsonObject("model"), cmd, context, models, notes);
        }
        return new Resolved(definition, models, notes);
    }

    private void pick(JsonObject node, JsonObject cmd, String context, List<Model> out, List<String> notes) {
        String type = strip(node.has("type") ? node.get("type").getAsString() : "model");
        String property = node.has("property") ? strip(node.get("property").getAsString()) : "";
        int index = node.has("index") ? node.get("index").getAsInt() : 0;
        switch (type) {
            case "model" -> {
                Model model = model(Identifier.parse(node.get("model").getAsString()), notes);
                if (model != null) {
                    out.add(model);
                }
            }
            case "composite" -> node.getAsJsonArray("models").forEach(m -> pick(m.getAsJsonObject(), cmd, context, out, notes));
            case "range_dispatch" -> {
                float value = 0;
                if (property.equals("custom_model_data")) {
                    JsonArray floats = cmd.has("floats") ? cmd.getAsJsonArray("floats") : new JsonArray();
                    value = index < floats.size() ? floats.get(index).getAsFloat() : 0;
                } else {
                    notes.add("range_dispatch on " + property + " read as 0");
                }
                value *= node.has("scale") ? node.get("scale").getAsFloat() : 1;
                JsonObject best = null;
                float threshold = Float.NEGATIVE_INFINITY;
                for (JsonElement entry : node.getAsJsonArray("entries")) {
                    float t = entry.getAsJsonObject().get("threshold").getAsFloat();
                    if (t <= value && t >= threshold) {
                        threshold = t;
                        best = entry.getAsJsonObject().getAsJsonObject("model");
                    }
                }
                pickOrFallback(best, node, cmd, context, out, notes);
            }
            case "select" -> {
                String value = null;
                if (property.equals("custom_model_data")) {
                    JsonArray strings = cmd.has("strings") ? cmd.getAsJsonArray("strings") : new JsonArray();
                    value = index < strings.size() ? strings.get(index).getAsString() : null;
                } else if (property.equals("display_context")) {
                    value = context;
                } else {
                    notes.add("select on " + property + " took its fallback");
                }
                JsonObject chosen = null;
                for (JsonElement entry : node.getAsJsonArray("cases")) {
                    JsonElement when = entry.getAsJsonObject().get("when");
                    boolean matches = false;
                    if (value != null && when.isJsonArray()) {
                        for (JsonElement w : when.getAsJsonArray()) {
                            matches |= w.getAsString().equals(value);
                        }
                    } else if (value != null) {
                        matches = when.getAsString().equals(value);
                    }
                    if (matches) {
                        chosen = entry.getAsJsonObject().getAsJsonObject("model");
                        break;
                    }
                }
                pickOrFallback(chosen, node, cmd, context, out, notes);
            }
            case "condition" -> {
                boolean value = false;
                if (property.equals("custom_model_data")) {
                    JsonArray flags = cmd.has("flags") ? cmd.getAsJsonArray("flags") : new JsonArray();
                    value = index < flags.size() && flags.get(index).getAsBoolean();
                } else {
                    notes.add("condition on " + property + " read as false");
                }
                pick(node.getAsJsonObject(value ? "on_true" : "on_false"), cmd, context, out, notes);
            }
            case "empty" -> {
            }
            default -> notes.add("item model type " + type + " is drawn in code (special or bundle), not exported");
        }
    }

    private void pickOrFallback(@Nullable JsonObject chosen, JsonObject node, JsonObject cmd, String context, List<Model> out, List<String> notes) {
        if (chosen != null) {
            pick(chosen, cmd, context, out, notes);
        } else if (node.has("fallback")) {
            pick(node.getAsJsonObject("fallback"), cmd, context, out, notes);
        } else {
            notes.add("no case matched and no fallback: draws the missing model");
        }
    }

    private @Nullable Model model(Identifier id, List<String> notes) {
        List<Identifier> chain = new ArrayList<>();
        Map<String, String> variables = new LinkedHashMap<>();
        JsonObject display = new JsonObject();
        JsonArray elements = null;
        boolean generated = false;
        Identifier current = id;
        while (current != null && chain.size() < 32) {
            if (current.getPath().equals("builtin/generated")) {
                generated = true;
                break;
            }
            Optional<JsonObject> json = read(current.withPath(path -> "models/" + path + ".json"));
            if (json.isEmpty()) {
                if (chain.isEmpty()) {
                    notes.add("missing model " + id);
                    return null;
                }
                notes.add("missing parent " + current + " of " + id);
                break;
            }
            chain.add(current);
            JsonObject model = json.get();
            if (model.has("textures")) {
                for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        variables.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                    } else if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("sprite")) {
                        variables.putIfAbsent(entry.getKey(), entry.getValue().getAsJsonObject().get("sprite").getAsString());
                    }
                }
            }
            if (model.has("display")) {
                for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("display").entrySet()) {
                    if (!display.has(entry.getKey())) {
                        display.add(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (elements == null && model.has("elements")) {
                elements = model.getAsJsonArray("elements");
            }
            current = model.has("parent") ? Identifier.parse(model.get("parent").getAsString()) : null;
        }
        Map<String, Identifier> textures = new LinkedHashMap<>();
        for (String key : variables.keySet()) {
            String value = variables.get(key);
            for (int depth = 0; value != null && value.startsWith("#") && depth < 16; depth++) {
                value = variables.get(value.substring(1));
            }
            if (value != null && !value.startsWith("#")) {
                textures.put(key, Identifier.parse(value));
            }
        }
        if (elements != null) {
            generated = false;
        } else if (!generated) {
            notes.add(id + " has no elements and is not a generated item");
        }
        return new Model(id, chain, textures, elements, display, generated);
    }

    /** The texture a face's {@code #variable} (or plain id) names, or null when it does not resolve. */
    static @Nullable Identifier texture(Model model, String reference) {
        if (reference.startsWith("#")) {
            return model.textures().get(reference.substring(1));
        }
        return Identifier.tryParse(reference);
    }

    private Optional<JsonObject> read(Identifier file) {
        return files.computeIfAbsent(file, f -> {
            Optional<Resource> resource = resources.getResource(f);
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            assets.copy(f);
            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                return json.isJsonObject() ? Optional.of(json.getAsJsonObject()) : Optional.empty();
            } catch (IOException | RuntimeException e) {
                UiCaptureClient.LOGGER.warn("Could not read {}", f, e);
                return Optional.empty();
            }
        });
    }

    private static String strip(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }
}
