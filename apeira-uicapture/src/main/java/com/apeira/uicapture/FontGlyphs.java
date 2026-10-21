package com.apeira.uicapture;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Finds the glyph images behind drawn text. Servers draw custom GUIs as text in resource pack fonts (private-use
 * characters mapped to bitmaps, with negative spaces to move them), so for every font a capture drew with this reads
 * the font's definition from every pack (following {@code reference} providers), and for each drawn codepoint lists
 * the bitmap that holds it (file, ascent, height, row and column in the sheet, copied into the capture) or its space
 * advance. Every matching provider is listed; the game uses the first in its load order.
 */
final class FontGlyphs {
    private FontGlyphs() {}

    static JsonObject describe(ResourceManager resources, Identifier font, Set<Integer> codepoints, CapturedAssets assets) {
        List<Provider> providers = new ArrayList<>();
        JsonArray definitions = new JsonArray();
        load(resources, font, providers, definitions, assets, new HashSet<>());
        JsonObject glyphs = new JsonObject();
        for (int codepoint : codepoints) {
            JsonArray matches = new JsonArray();
            for (Provider provider : providers) {
                JsonObject match = provider.match(codepoint, assets);
                if (match != null) {
                    matches.add(match);
                }
            }
            glyphs.add(UiCapture.hex(codepoint), matches);
        }
        JsonObject out = new JsonObject();
        out.add("definitions", definitions);
        out.add("glyphs", glyphs);
        return out;
    }

    private static void load(ResourceManager resources, Identifier font, List<Provider> providers, JsonArray definitions,
            CapturedAssets assets, Set<Identifier> seen) {
        if (!seen.add(font)) {
            return;
        }
        Identifier file = font.withPath(path -> "font/" + path + ".json");
        assets.copy(file);
        for (Resource resource : resources.getResourceStack(file)) {
            JsonObject definition = new JsonObject();
            definition.addProperty("font", font.toString());
            definition.addProperty("pack", resource.sourcePackId());
            definitions.add(definition);
            try (Reader reader = resource.openAsReader()) {
                for (JsonElement element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("providers")) {
                    JsonObject provider = element.getAsJsonObject();
                    if ("reference".equals(type(provider))) {
                        load(resources, Identifier.parse(provider.get("id").getAsString()), providers, definitions, assets, seen);
                    } else {
                        providers.add(new Provider(provider, resource.sourcePackId(), font));
                    }
                }
            } catch (IOException | JsonParseException | IllegalStateException | NullPointerException e) {
                definition.addProperty("error", String.valueOf(e.getMessage()));
            }
        }
    }

    private static String type(JsonObject provider) {
        return provider.has("type") ? provider.get("type").getAsString() : "";
    }

    private record Provider(JsonObject json, String pack, Identifier font) {
        JsonObject match(int codepoint, CapturedAssets assets) {
            return switch (type(json)) {
                case "bitmap" -> bitmap(codepoint, assets);
                case "space" -> space(codepoint);
                default -> null;
            };
        }

        private JsonObject bitmap(int codepoint, CapturedAssets assets) {
            JsonArray rows = json.getAsJsonArray("chars");
            if (rows == null) {
                return null;
            }
            int columns = 0;
            for (JsonElement row : rows) {
                columns = Math.max(columns, (int) row.getAsString().codePoints().count());
            }
            for (int r = 0; r < rows.size(); r++) {
                int[] chars = rows.get(r).getAsString().codePoints().toArray();
                for (int c = 0; c < chars.length; c++) {
                    if (chars[c] == codepoint) {
                        JsonObject match = header("bitmap");
                        String file = json.get("file").getAsString();
                        Identifier texture = Identifier.parse(file).withPrefix("textures/");
                        match.addProperty("file", file);
                        match.addProperty("texture", texture.toString());
                        assets.copy(texture);
                        match.addProperty("ascent", json.has("ascent") ? json.get("ascent").getAsInt() : 0);
                        match.addProperty("height", json.has("height") ? json.get("height").getAsInt() : 8);
                        match.addProperty("row", r);
                        match.addProperty("column", c);
                        match.addProperty("rows", rows.size());
                        match.addProperty("columns", columns);
                        return match;
                    }
                }
            }
            return null;
        }

        private JsonObject space(int codepoint) {
            JsonObject advances = json.getAsJsonObject("advances");
            if (advances == null) {
                return null;
            }
            String key = Character.toString(codepoint);
            if (!advances.has(key)) {
                return null;
            }
            JsonObject match = header("space");
            match.add("advance", advances.get(key));
            return match;
        }

        private JsonObject header(String type) {
            JsonObject match = new JsonObject();
            match.addProperty("type", type);
            match.addProperty("pack", pack);
            match.addProperty("font", font.toString());
            return match;
        }
    }
}
