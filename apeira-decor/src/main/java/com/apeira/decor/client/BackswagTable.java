package com.apeira.decor.client;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.ApeiraDecor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Back cosmetics worn the way a server wears them: the item's {@code head} display on an invisible armor stand riding
 * the player, {@link Entry#dy} blocks above the player's feet. The heights come from the UI capture tool's backswag
 * logger ({@code tools/uicapture-fabric-26.3}, {@code BackswagLogger}), whose table is keyed by the cosmetic's name in
 * snake case, which is the Apeira item id's path ({@code monkey_backswag}):
 * <ol>
 * <li>{@code assets/apeira/backswag/offsets.json}, a copy of that table shipped with decor;</li>
 * <li>{@code logs/apeira_backswag_offsets.json} in the game directory, the logger's own output, read over the shipped
 * one when present and read again when it changes, so a fresh log shows at once in a dev run.</li>
 * </ol>
 * Each entry gives {@code dy} (or {@code dy_scaled}, the height for a wearer of scale 1), the stand's {@code forward}
 * and {@code right} offsets from the wearer, {@code small} (a small stand's head is lower and half size), and its
 * {@code cosmetic}'s or {@code worn} item's {@code default_paint} and {@code lore}. A back cosmetic without an entry
 * keeps CosmoLib's on-the-body transform.
 */
public final class BackswagTable {
    private static final Identifier SHIPPED = ApeiraIds.id("backswag/offsets.json");
    private static final String LOGGED = "logs/apeira_backswag_offsets.json";
    private static final long CHECK_MILLIS = 1000L;

    /**
     * One logged backswag: the stand's offset from the wearer's feet, in blocks (forward and right in the wearer's
     * facing), whether it was a small stand, and the logged item's first firework colour and lore.
     */
    public record Entry(double dy, double forward, double right, boolean small, @Nullable Integer defaultPaint, List<String> lore) {}

    private static Map<Item, Entry> entries = Map.of();
    private static boolean shippedLoaded;
    private static Map<String, Entry> shipped = Map.of();
    private static long loggedModified = Long.MIN_VALUE;
    private static long lastCheck;

    private BackswagTable() {}

    /** {@code item}'s logged backswag, or {@code null} when none was logged. */
    public static @Nullable Entry get(Item item) {
        refresh();
        return entries.get(item);
    }

    private static void refresh() {
        long now = Util.getMillis();
        if (shippedLoaded && now - lastCheck < CHECK_MILLIS) {
            return;
        }
        lastCheck = now;
        boolean changed = false;
        if (!shippedLoaded) {
            shippedLoaded = true;
            shipped = readShipped();
            changed = true;
        }
        Path logged = FMLPaths.GAMEDIR.get().resolve(LOGGED);
        long modified = modified(logged);
        if (modified != loggedModified) {
            loggedModified = modified;
            changed = true;
        }
        if (changed) {
            Map<String, Entry> byName = new HashMap<>(shipped);
            if (modified != Long.MIN_VALUE) {
                try {
                    byName.putAll(parse(JsonParser.parseString(Files.readString(logged, StandardCharsets.UTF_8))));
                } catch (Exception e) {
                    ApeiraDecor.LOGGER.warn("Could not read {}", logged, e);
                }
            }
            Map<Item, Entry> resolved = new HashMap<>();
            byName.forEach((name, entry) -> BuiltInRegistries.ITEM.getOptional(ApeiraIds.id(name))
                    .ifPresent(item -> resolved.put(item, entry)));
            entries = resolved;
        }
    }

    private static Map<String, Entry> readShipped() {
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(SHIPPED)) {
            return parse(JsonParser.parseReader(reader));
        } catch (Exception e) {
            ApeiraDecor.LOGGER.debug("No shipped backswag table {}", SHIPPED);
            return Map.of();
        }
    }

    private static long modified(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() : Long.MIN_VALUE;
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    /** The logger's table: {@code {"<snake_case name>": {"dy": ..., "cosmetic": {...}, "worn": {...}}, ...}}. */
    static Map<String, Entry> parse(JsonElement root) {
        Map<String, Entry> parsed = new HashMap<>();
        if (!root.isJsonObject()) {
            return parsed;
        }
        for (Map.Entry<String, JsonElement> row : root.getAsJsonObject().entrySet()) {
            if (!row.getValue().isJsonObject()) {
                continue;
            }
            JsonObject entry = row.getValue().getAsJsonObject();
            JsonObject worn = object(entry, "worn");
            JsonObject cosmetic = object(entry, "cosmetic");
            double dy = number(entry, "dy_scaled", number(worn, "dy_scaled", number(entry, "dy", Double.NaN)));
            if (Double.isNaN(dy)) {
                continue;
            }
            double forward = number(entry, "forward", number(worn, "forward", 0.0));
            double right = number(entry, "right", number(worn, "right", 0.0));
            boolean small = entry.has("small") && entry.get("small").getAsBoolean();
            Integer paint = paint(cosmetic);
            if (paint == null) {
                paint = paint(worn);
            }
            List<String> lore = lore(cosmetic);
            parsed.put(row.getKey().toLowerCase(Locale.ROOT), new Entry(dy, forward, right, small, paint, lore));
        }
        return parsed;
    }

    private static @Nullable JsonObject object(@Nullable JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    private static double number(@Nullable JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsDouble() : fallback;
    }

    /** {@code default_paint}, {@code "#RRGGBB"}. */
    private static @Nullable Integer paint(@Nullable JsonObject item) {
        if (item == null || !item.has("default_paint") || !item.get("default_paint").isJsonPrimitive()) {
            return null;
        }
        String hex = item.get("default_paint").getAsString().replace("#", "");
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The hovered item's tooltip lines after its name ({@code tooltip}, a list of strings), when logged. */
    private static List<String> lore(@Nullable JsonObject item) {
        List<String> lines = new ArrayList<>();
        if (item != null && item.has("tooltip") && item.get("tooltip").isJsonArray()) {
            item.getAsJsonArray("tooltip").forEach(line -> {
                if (line.isJsonPrimitive()) {
                    lines.add(line.getAsString());
                }
            });
            if (!lines.isEmpty()) {
                lines.removeFirst();
            }
        }
        return List.copyOf(lines);
    }
}
