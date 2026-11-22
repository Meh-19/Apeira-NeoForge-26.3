package com.apeira.decor.furniture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Furniture tuner results applied to code: {@code apeira_decor/furniture_tunes.json} (decor's resources), a JSON object
 * of {@link FurnitureTuning}s keyed by block id ({@code "apeira:bathtub"}), each naming only what it changes
 * ({@code removed_boxes}, {@code removed_diagonal_boxes}, {@code seats}, {@code light}, {@code collision}). Pieces of
 * every module are listed here, as the file is read by id. {@code scripts/apply_furniture_tunes.py} moves entries of
 * apeira-dev's tune log into it and deletes them from the log.
 */
public final class FurnitureTunes {
    public static final String RESOURCE = "/apeira_decor/furniture_tunes.json";
    private static final JsonObject FILE = read();
    /** Each piece's tune, looked up once: light and collision are asked for often. */
    private static final Map<FurnitureBlock, Optional<FurnitureTuning>> PARSED = new ConcurrentHashMap<>();

    private FurnitureTunes() {}

    private static JsonObject read() {
        try (InputStream stream = FurnitureTunes.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return new JsonObject();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Could not read " + RESOURCE, e);
        }
    }

    /** {@code block}'s baked tune, or {@code null} when its code values are its own. */
    public static @Nullable FurnitureTuning get(FurnitureBlock block) {
        return PARSED.computeIfAbsent(block, b -> {
            JsonElement entry = FILE.get(BuiltInRegistries.BLOCK.getKey(b).toString());
            return entry != null && entry.isJsonObject() ? Optional.of(FurnitureTuning.fromJson(entry.getAsJsonObject(), b)) : Optional.empty();
        }).orElse(null);
    }
}
