package com.apeira.decor.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

/**
 * A skin's accent colour, for backgrounds behind a player (Origin Realms tints its profile panel with one). The face
 * comes first ({@link #face}): the 8x8 front of the head with the hat layer's opaque pixels over it. Its most common
 * colour bucket is the skin tone; every other bucket is a feature (eyes, mouth, markings, a mask) and scores how far it
 * is from the skin tone, its saturation, how close its lightness is to 50% and how many pixels it covers, with pixels
 * in the eye rows (face rows 3-4, the skin's y 11-12) counting three times. The best feature's average colour wins, so
 * a player is shown on the colour of their eyes, or of the most distinct thing on their face.
 *
 * <p>A face of one colour (or one whose features are only near-black or near-white outlines) falls back on the
 * {@link #REGIONS} pass: the head's side strip (front, sides and back of the head and hat layer) and the body with its
 * jacket, arms, legs and the head's top and bottom left out, scored by saturation, lightness and the square root of
 * their share ({@link #dominant}). Buckets are 3 bits per channel, so shades of one colour count together.
 *
 * <p>Results are kept per skin texture, so a skin is read once; a profile whose skin has not loaded yet shows the
 * default skin's accent until it has.
 */
public final class SkinAccent {
    /** A neutral slate, for skins that cannot be read. */
    public static final int FALLBACK = 0xFF3D4A5C;
    private static final Map<Identifier, Integer> CACHE = new HashMap<>();
    /**
     * The sampled areas of a 64x64 skin, {x0, y0, x1, y1} (end exclusive): rows 8-16 across the whole width (head and
     * hat sides) and x 16-40 over rows 16-48 (body, then jacket). Scaled for HD skins; a legacy 64x32 skin has no jacket
     * rows, so the body box stops at its bottom.
     */
    private static final int[][] REGIONS = {{0, 8, 64, 16}, {16, 16, 40, 48}};
    /** The face (head front) and the hat layer's front, {x, y} of their top left corners on a 64x64 skin; 8x8 each. */
    private static final int FACE_X = 8;
    private static final int HAT_X = 40;
    private static final int FACE_Y = 8;
    /** Face rows (0-7) where skins draw their eyes; their pixels count {@link #EYE_WEIGHT} times. */
    private static final int EYE_ROW_MIN = 3;
    private static final int EYE_ROW_MAX = 4;
    private static final int EYE_WEIGHT = 3;
    /** A feature closer than this to the skin tone (RGB distance, 0-441) is a shade of it, not a feature. */
    private static final double MIN_DISTANCE = 40.0;
    /** A feature scoring below this (a black or white outline, a speck) leaves the choice to the body pass. */
    private static final double MIN_FEATURE_SCORE = 0.02;

    private SkinAccent() {}

    /** The accent colour of the skin texture {@code texture}, opaque ARGB. */
    public static int of(Identifier texture) {
        Integer cached = CACHE.get(texture);
        if (cached != null) {
            return cached;
        }
        NativeImage image = image(texture);
        if (image == null) {
            // Not readable (yet): try again next time instead of keeping the fallback.
            return FALLBACK;
        }
        int accent;
        try {
            Integer feature = feature(face(image));
            accent = feature != null ? feature : dominant(read(image), FALLBACK);
        } finally {
            // The downloaded texture's own pixels stay with it; only a copy read from the packs is closed.
            if (!(Minecraft.getInstance().getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic
                    && dynamic.getPixels() == image)) {
                image.close();
            }
        }
        CACHE.put(texture, accent);
        return accent;
    }

    /**
     * The face as 64 ARGB pixels, row by row: each of the head front's pixels, or the hat layer's over it where that is
     * opaque. An HD skin's face is read at one pixel per skin pixel of a 64x64 skin (its top left texel of each cell).
     */
    static int[] face(NativeImage image) {
        int scale = Math.max(1, image.getWidth() / 64);
        int[] face = new int[64];
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                int y = (FACE_Y + row) * scale;
                int pixel = image.getPixel((FACE_X + column) * scale, y);
                int hat = image.getPixel((HAT_X + column) * scale, y);
                face[row * 8 + column] = ARGB.alpha(hat) >= 128 ? hat : pixel;
            }
        }
        return face;
    }

    /**
     * The face's most distinct feature colour (see the class comment), or null when it has none worth a background:
     * every opaque pixel is the skin tone or a shade of it, or the best feature scores under {@link #MIN_FEATURE_SCORE}.
     */
    static @Nullable Integer feature(int[] face) {
        // Per bucket: weighted pixel count and the weighted sums of red, green and blue.
        Map<Integer, long[]> buckets = new HashMap<>();
        long total = 0;
        for (int i = 0; i < face.length; i++) {
            int pixel = face[i];
            if (ARGB.alpha(pixel) < 128) {
                continue;
            }
            int row = i / 8;
            int weight = row >= EYE_ROW_MIN && row <= EYE_ROW_MAX ? EYE_WEIGHT : 1;
            int r = ARGB.red(pixel);
            int g = ARGB.green(pixel);
            int b = ARGB.blue(pixel);
            long[] bucket = buckets.computeIfAbsent(bucket(r, g, b), key -> new long[5]);
            bucket[0] += weight;
            bucket[1] += (long) r * weight;
            bucket[2] += (long) g * weight;
            bucket[3] += (long) b * weight;
            // Unweighted count, for the skin tone, which is the face's commonest colour wherever it is drawn.
            bucket[4]++;
            total += weight;
        }
        long[] tone = null;
        for (long[] bucket : buckets.values()) {
            if (tone == null || bucket[4] > tone[4]) {
                tone = bucket;
            }
        }
        if (tone == null) {
            return null;
        }
        double toneR = tone[1] / (double) tone[0];
        double toneG = tone[2] / (double) tone[0];
        double toneB = tone[3] / (double) tone[0];
        double bestScore = MIN_FEATURE_SCORE;
        Integer best = null;
        for (long[] bucket : buckets.values()) {
            if (bucket == tone) {
                continue;
            }
            int r = (int) (bucket[1] / bucket[0]);
            int g = (int) (bucket[2] / bucket[0]);
            int b = (int) (bucket[3] / bucket[0]);
            double distance = Math.sqrt(square(r - toneR) + square(g - toneG) + square(b - toneB));
            if (distance < MIN_DISTANCE) {
                continue;
            }
            double score = score(r, g, b, bucket[0] / (double) total) * (distance / 441.7);
            if (score > bestScore) {
                bestScore = score;
                best = ARGB.color(255, r, g, b);
            }
        }
        return best;
    }

    private static int bucket(int r, int g, int b) {
        return (r >> 5) << 6 | (g >> 5) << 3 | b >> 5;
    }

    private static double square(double value) {
        return value * value;
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
            long[] bucket = buckets.computeIfAbsent(bucket(r, g, b), key -> new long[4]);
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

    /**
     * The skin's image: its downloaded texture's own pixels (owned by the texture, never closed here), else a copy read
     * from the resource packs (closed by {@link #of}), else null.
     */
    private static @Nullable NativeImage image(Identifier id) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractTexture texture = minecraft.getTextureManager().getTexture(id);
        if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return dynamic.getPixels();
        }
        Optional<Resource> resource = minecraft.getResourceManager().getResource(id);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                return NativeImage.read(in);
            } catch (IOException exception) {
                return null;
            }
        }
        return null;
    }

    /** The pixels of the skin's {@link #REGIONS}. */
    private static int[] read(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int scale = Math.max(1, width / 64);
        IntStream.Builder pixels = IntStream.builder();
        for (int[] region : REGIONS) {
            int y1 = Math.min(region[3] * scale, height);
            for (int y = region[1] * scale; y < y1; y++) {
                for (int x = region[0] * scale; x < Math.min(region[2] * scale, width); x++) {
                    pixels.add(image.getPixel(x, y));
                }
            }
        }
        return pixels.build().toArray();
    }
}
