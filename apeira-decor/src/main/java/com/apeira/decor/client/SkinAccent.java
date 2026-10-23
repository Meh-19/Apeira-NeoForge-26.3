package com.apeira.decor.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

/**
 * A skin's accent colour, for backgrounds behind a player (Origin Realms tints its profile panel with one). The skin's
 * opaque pixels are grouped into colour buckets (3 bits per channel, so shades of one colour count together), and each
 * bucket scores its saturation, how close its lightness is to 50% (very dark and very light colours make poor
 * backgrounds) and how much of the skin it covers; the best bucket's average colour wins.
 *
 * <p>Results are kept per skin texture, so a skin is read once; a profile whose skin has not loaded yet shows the
 * default skin's accent until it has.
 */
public final class SkinAccent {
    /** A neutral slate, for skins that cannot be read. */
    public static final int FALLBACK = 0xFF3D4A5C;
    private static final Map<Identifier, Integer> CACHE = new HashMap<>();

    private SkinAccent() {}

    /** The accent colour of the skin texture {@code texture}, opaque ARGB. */
    public static int of(Identifier texture) {
        Integer cached = CACHE.get(texture);
        if (cached != null) {
            return cached;
        }
        int[] pixels = pixels(texture);
        if (pixels == null) {
            // Not readable (yet): try again next time instead of keeping the fallback.
            return FALLBACK;
        }
        int accent = dominant(pixels, FALLBACK);
        CACHE.put(texture, accent);
        return accent;
    }

    /** The best-scoring colour of {@code pixels} (ARGB), or {@code fallback} when none is opaque. */
    public static int dominant(int[] pixels, int fallback) {
        // Per bucket: pixel count and the sums of red, green and blue.
        Map<Integer, long[]> buckets = new HashMap<>();
        int total = 0;
        for (int pixel : pixels) {
            if (ARGB.alpha(pixel) < 128) {
                continue;
            }
            int r = ARGB.red(pixel);
            int g = ARGB.green(pixel);
            int b = ARGB.blue(pixel);
            long[] bucket = buckets.computeIfAbsent((r >> 5) << 6 | (g >> 5) << 3 | b >> 5, key -> new long[4]);
            bucket[0]++;
            bucket[1] += r;
            bucket[2] += g;
            bucket[3] += b;
            total++;
        }
        double bestScore = -1.0;
        int best = fallback;
        for (long[] bucket : buckets.values()) {
            int r = (int) (bucket[1] / bucket[0]);
            int g = (int) (bucket[2] / bucket[0]);
            int b = (int) (bucket[3] / bucket[0]);
            double score = score(r, g, b, bucket[0] / (double) total);
            if (score > bestScore) {
                bestScore = score;
                best = ARGB.color(255, r, g, b);
            }
        }
        return best;
    }

    /**
     * A colour's score: saturation (with a floor, so a grey skin still picks its most common grey), closeness of its
     * lightness to 50%, and the share of the skin it covers (square-rooted, so a large dull area does not drown out a
     * smaller vivid one).
     */
    static double score(int r, int g, int b, double share) {
        double max = Math.max(r, Math.max(g, b)) / 255.0;
        double min = Math.min(r, Math.min(g, b)) / 255.0;
        double lightness = (max + min) / 2.0;
        double saturation = max == min ? 0.0 : (max - min) / (1.0 - Math.abs(2.0 * lightness - 1.0));
        double balance = Math.max(0.05, 1.0 - Math.abs(lightness - 0.5) * 2.0);
        return (0.15 + saturation) * balance * Math.sqrt(share);
    }

    /** The skin's pixels: from its downloaded texture, else from the resource packs, else null. */
    private static int @Nullable [] pixels(Identifier id) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractTexture texture = minecraft.getTextureManager().getTexture(id);
        if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return read(dynamic.getPixels());
        }
        Optional<Resource> resource = minecraft.getResourceManager().getResource(id);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                return read(image);
            } catch (IOException exception) {
                return null;
            }
        }
        return null;
    }

    private static int[] read(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = image.getPixel(x, y);
            }
        }
        return pixels;
    }
}
