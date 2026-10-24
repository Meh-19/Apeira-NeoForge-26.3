package com.apeira.uicapture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Packs the textures an export uses into one image, as GeckoLib draws a model with a single texture: each texture's first
 * animation frame (an {@code .mcmeta} animation's frame size, else the image's width squared), shelf-packed by height.
 * {@link #pin} puts a texture at the atlas's top left corner first (the armor stand's, so its box UV stays as it is).
 * Textures that do not exist become a 16x16 magenta and black square.
 */
final class TextureAtlas {
    /** Where a texture landed, in atlas pixels, and its first frame (kept for generated items' outlines). */
    record Slot(int x, int y, int width, int height, NativeImage image, boolean missing) {
        int alpha(int px, int py) {
            return image.getPixel(px, py) >>> 24;
        }
    }

    private final ResourceManager resources;
    private final CapturedAssets assets;
    private final Map<Identifier, Slot> slots = new LinkedHashMap<>();
    private @Nullable Identifier pinned;
    private int width;
    private int height;
    private @Nullable NativeImage atlas;

    TextureAtlas(ResourceManager resources, CapturedAssets assets) {
        this.resources = resources;
        this.assets = assets;
    }

    /** Adds a texture by id ({@code minecraft:item/apple}, read from {@code textures/item/apple.png}); returns its frame. */
    Slot add(Identifier texture) {
        return slots.computeIfAbsent(texture, this::load);
    }

    /** Adds the first texture file of {@code files} that exists and keeps it at the top left corner; returns whether one did. */
    boolean pin(Identifier... files) {
        for (Identifier file : files) {
            if (resources.getResource(file).isPresent()) {
                Identifier id = file.withPath(path -> path.replaceFirst("^textures/", "").replaceFirst("\\.png$", ""));
                add(id);
                pinned = id;
                return true;
            }
        }
        return false;
    }

    boolean isEmpty() {
        return slots.isEmpty();
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    Slot slot(Identifier texture) {
        return slots.get(texture);
    }

    Map<Identifier, Slot> slots() {
        return slots;
    }

    private Slot load(Identifier texture) {
        Identifier file = texture.withPath(path -> "textures/" + path + ".png");
        Optional<Resource> resource = resources.getResource(file);
        if (resource.isPresent()) {
            assets.copy(file);
            try (InputStream in = resource.get().open()) {
                NativeImage image = NativeImage.read(in);
                int w = image.getWidth();
                int h = Math.min(image.getHeight(), frameHeight(file, w));
                return new Slot(0, 0, w, h, image, false);
            } catch (IOException | RuntimeException e) {
                UiCaptureClient.LOGGER.warn("Could not read texture {}", file, e);
            }
        }
        NativeImage missing = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                missing.setPixel(x, y, (x < 8) == (y < 8) ? 0xFFF800F8 : 0xFF000000);
            }
        }
        return new Slot(0, 0, 16, 16, missing, true);
    }

    /** An animated texture's frame height: the {@code .mcmeta} animation's {@code height}, else its {@code width} or the image's width. */
    private int frameHeight(Identifier file, int imageWidth) {
        Optional<Resource> meta = resources.getResource(file.withPath(file.getPath() + ".mcmeta"));
        if (meta.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try (Reader reader = new InputStreamReader(meta.get().open(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("animation")) {
                return Integer.MAX_VALUE;
            }
            JsonObject animation = json.getAsJsonObject("animation");
            if (animation.has("height")) {
                return animation.get("height").getAsInt();
            }
            return animation.has("width") ? animation.get("width").getAsInt() : imageWidth;
        } catch (IOException | RuntimeException e) {
            return Integer.MAX_VALUE;
        }
    }

    /** Places every texture and draws the atlas; call once, after every {@link #add}. */
    void pack() {
        List<Map.Entry<Identifier, Slot>> order = new ArrayList<>(slots.entrySet());
        order.sort(Comparator.comparing((Map.Entry<Identifier, Slot> e) -> !e.getKey().equals(pinned))
                .thenComparing(e -> -e.getValue().height()));
        long area = 0;
        int widest = 16;
        for (Slot slot : slots.values()) {
            area += (long) slot.width() * slot.height();
            widest = Math.max(widest, slot.width());
        }
        // The power of two at least as wide as the widest texture and the square root of the area.
        width = Integer.highestOneBit(Math.max(widest, (int) Math.ceil(Math.sqrt(area))) - 1) * 2;
        int x = 0, y = 0, row = 0;
        Map<Identifier, Slot> placed = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Slot> entry : order) {
            Slot slot = entry.getValue();
            if (x + slot.width() > width) {
                x = 0;
                y += row;
                row = 0;
            }
            placed.put(entry.getKey(), new Slot(x, y, slot.width(), slot.height(), slot.image(), slot.missing()));
            x += slot.width();
            row = Math.max(row, slot.height());
        }
        height = Math.max(16, y + row);
        slots.clear();
        slots.putAll(placed);
        atlas = new NativeImage(width, height, true);
        for (Slot slot : slots.values()) {
            for (int py = 0; py < slot.height(); py++) {
                for (int px = 0; px < slot.width(); px++) {
                    atlas.setPixel(slot.x() + px, slot.y() + py, slot.image().getPixel(px, py));
                }
            }
        }
    }

    void write(Path file) throws IOException {
        if (atlas != null) {
            atlas.writeToFile(file);
        }
    }

    void close() {
        slots.values().forEach(slot -> slot.image().close());
        if (atlas != null) {
            atlas.close();
        }
    }
}
