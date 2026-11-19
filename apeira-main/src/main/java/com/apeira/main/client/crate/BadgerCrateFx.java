package com.apeira.main.client.crate;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;
import com.apeira.main.crate.CrateKind;
import com.apeira.main.crate.BadgerTier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The Badger Crate model's texture with each reward's VFX in its rolled tier's colour. The exporter gave every reward's
 * glow, pulse, trail and spark frames their own regions of the atlas ({@code badger_crate/fx_layout.json}); this paints
 * them with {@code textures/entity/badger_crate/vfx/<colour>_<effect>.png} (sparks from {@code <colour>_spark/}) and
 * plays the pulse textures' frames, as a {@link DynamicTexture} registered as {@link #TEXTURE}.
 */
final class BadgerCrateFx {
    static final Identifier TEXTURE = ApeiraIds.id("dynamic/badger_crate_fx");
    private static final Identifier LAYOUT = ApeiraIds.id("badger_crate/fx_layout.json");
    private static final int LAST_SPARK_FRAME = 8;

    private record Region(int slot, String effect, int frame, int x, int y, int w, int h) {}

    private static @Nullable DynamicTexture texture;
    private static @Nullable NativeImage base;
    private static List<Region> regions = List.of();
    private static final Map<String, NativeImage> SOURCES = new HashMap<>();
    private static int paintedTiers = -1;
    private static int paintedPulseFrame = -1;
    /** The crate whose atlas ({@link CrateKind#texture}) the texture was made from. */
    private static @Nullable CrateKind paintedKind;

    private BadgerCrateFx() {}

    /** True once the texture exists; the renderer falls back to the crate's atlas until then. */
    static boolean ready() {
        return texture != null;
    }

    /** Paints the three rewards' tiers (and the pulse frame for {@code age}), uploading only what changed. */
    static void update(CrateKind kind, BadgerTier[] tiers, int age) {
        if (texture != null && paintedKind != kind) {
            release();
        }
        if (texture == null && !create(kind)) {
            return;
        }
        int key = tiers[0].ordinal() | tiers[1].ordinal() << 2 | tiers[2].ordinal() << 4;
        int pulseFrame = age;
        boolean dirty = false;
        for (Region region : regions) {
            boolean pulse = region.effect.equals("pulse");
            if (key == paintedTiers && (!pulse || pulseFrame == paintedPulseFrame)) {
                continue;
            }
            BadgerTier tier = tiers[Math.clamp(region.slot - 1, 0, 2)];
            paint(region, tier, pulseFrame);
            dirty = true;
        }
        paintedTiers = key;
        paintedPulseFrame = pulseFrame;
        if (dirty) {
            texture.upload();
        }
    }

    /** Frees the texture once the opening is over. */
    static void release() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE);
            texture = null;
        }
        if (base != null) {
            base.close();
            base = null;
        }
        SOURCES.values().forEach(NativeImage::close);
        SOURCES.clear();
        paintedTiers = -1;
        paintedPulseFrame = -1;
        paintedKind = null;
    }

    private static boolean create(CrateKind kind) {
        try {
            base = read(kind.texture());
            if (base == null) {
                return false;
            }
            List<Region> loaded = new ArrayList<>();
            try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(LAYOUT)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (JsonElement element : root.getAsJsonArray("regions")) {
                    JsonObject r = element.getAsJsonObject();
                    loaded.add(new Region(r.get("slot").getAsInt(), r.get("effect").getAsString(), r.get("frame").getAsInt(),
                            r.get("x").getAsInt(), r.get("y").getAsInt(), r.get("w").getAsInt(), r.get("h").getAsInt()));
                }
            }
            regions = List.copyOf(loaded);
            NativeImage pixels = new NativeImage(base.getWidth(), base.getHeight(), false);
            pixels.copyFrom(base);
            texture = new DynamicTexture(() -> "Badger Crate VFX", pixels);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
            paintedKind = kind;
            return true;
        } catch (Exception e) {
            Apeira.LOGGER.warn("Could not make the Badger Crate VFX texture", e);
            release();
            return false;
        }
    }

    private static void paint(Region region, BadgerTier tier, int age) {
        String colour = tier.colour();
        Identifier source = switch (region.effect) {
            case "spark" -> ApeiraIds.id("textures/entity/badger_crate/vfx/" + colour + "_spark/" + colour + "_spark_"
                    + Math.min(region.frame, LAST_SPARK_FRAME) + ".png");
            default -> ApeiraIds.id("textures/entity/badger_crate/vfx/" + colour + "_" + region.effect + ".png");
        };
        NativeImage image = SOURCES.computeIfAbsent(source.toString(), key -> read(source));
        NativeImage target = texture.getPixels();
        if (image == null || target == null) {
            return;
        }
        // Animated strips (the pulses: square frames stacked) show one frame per tick.
        int frameSize = image.getWidth();
        int frames = Math.max(1, image.getHeight() / frameSize);
        int frameTop = frames > 1 ? Math.floorMod(age, frames) * frameSize : 0;
        int frameHeight = frames > 1 ? frameSize : image.getHeight();
        for (int dy = 0; dy < region.h; dy++) {
            int sy = frameTop + Math.min(frameHeight - 1, dy * frameHeight / region.h);
            for (int dx = 0; dx < region.w; dx++) {
                int sx = Math.min(image.getWidth() - 1, dx * image.getWidth() / region.w);
                target.setPixel(region.x + dx, region.y + dy, image.getPixel(sx, sy));
            }
        }
    }

    private static @Nullable NativeImage read(Identifier id) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(id)) {
            return NativeImage.read(in);
        } catch (Exception e) {
            Apeira.LOGGER.debug("No Badger Crate texture {}", id);
            return null;
        }
    }
}
