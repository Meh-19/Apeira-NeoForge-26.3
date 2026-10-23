package com.apeira.main.client.navigator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.apeira.main.Apeira;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

/**
 * Where the navigator's buttons are actually drawn. Origin Realms' textures are not laid out on the chest's 18-pixel
 * slot grid: a button may start a few pixels into a slot, or span half of the next one. So instead of lighting up and
 * clicking whole slot cells, the screen finds each button's outline in the page's layers: from the middle of each of
 * the button's slots it fills outwards until it meets the black outline (or the edge of the texture) and takes the box
 * around what it filled, which is the button's face inside its outline. A slot whose fill escapes (an icon with no
 * outline, such as a close cross) falls back to its slot cell.
 *
 * <p>Layers are read once per screen from the resource manager and composited as {@link NavigatorLayout} draws them.
 */
final class NavigatorBounds {
    private static final int SIZE = 256;
    /** Layers are drawn at {@code (leftPos, topPos - 6)}; frame coordinates are relative to {@code (leftPos, topPos)}. */
    private static final int LAYER_Y = -6;
    /** How far past its slots a button may reach before the fill counts as escaped. */
    private static final int REACH = 8;
    /** How far from a slot's middle to look for a pixel that is not outline, when the middle is a dark icon pixel. */
    private static final int SEED_SEARCH = 5;

    private final Map<Identifier, int[]> layers = new HashMap<>();
    private List<Identifier> composedFor = List.of();
    private int[] composed = new int[SIZE * SIZE];

    /** A box in frame coordinates, {@code x0, y0} inclusive and {@code x1, y1} exclusive. */
    record Rect(int x0, int y0, int x1, int y1) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }

        Rect union(Rect other) {
            return new Rect(Math.min(x0, other.x0), Math.min(y0, other.y0), Math.max(x1, other.x1), Math.max(y1, other.y1));
        }
    }

    /**
     * The drawn faces of the button over {@code slots} on a page drawn from {@code pageLayers}: usually one box, but a
     * button made of separate cells (the free cells of the Homes grid) has one per cell.
     */
    List<Rect> measure(List<Identifier> pageLayers, int[] slots) {
        if (!pageLayers.equals(composedFor)) {
            compose(pageLayers);
        }
        Rect cells = null;
        for (int slot : slots) {
            cells = cells == null ? cell(slot) : cells.union(cell(slot));
        }
        List<Rect> faces = new ArrayList<>();
        if (cells == null) {
            return faces;
        }
        boolean[] seen = new boolean[SIZE * SIZE];
        for (int slot : slots) {
            int x = slotX(slot) + 8;
            int y = slotY(slot) + 8;
            if (faces.stream().anyMatch(face -> face.contains(x, y))) {
                continue;
            }
            Rect face = fill(seen, x, y - LAYER_Y, cells);
            faces.add(face != null ? face : cell(slot));
        }
        return List.copyOf(faces);
    }

    /** A slot's 18-pixel cell, the old highlight: the slot's item area and the pixel around it. */
    private static Rect cell(int slot) {
        return new Rect(slotX(slot) - 1, slotY(slot) - 1, slotX(slot) + 17, slotY(slot) + 17);
    }

    /** Fills from near {@code (cx, cy)} (texture coordinates); the filled box in frame coordinates, or null if it escaped. */
    private Rect fill(boolean[] seen, int cx, int cy, Rect cells) {
        int seed = -1;
        for (int d = 0; d <= SEED_SEARCH && seed < 0; d++) {
            int[][] offsets = {{d, 0}, {-d, 0}, {0, d}, {0, -d}};
            for (int[] offset : offsets) {
                int x = cx + offset[0];
                int y = cy + offset[1];
                if (inTexture(x, y) && !outline(x, y)) {
                    seed = y * SIZE + x;
                    break;
                }
            }
        }
        if (seed < 0 || seen[seed]) {
            // Only outline here, or a region already filled for this button (one that escaped): the caller uses the cell.
            return null;
        }
        int minX = cells.x0 - REACH;
        int maxX = cells.x1 + REACH;
        int minY = cells.y0 - LAYER_Y - REACH;
        int maxY = cells.y1 - LAYER_Y + REACH;
        int x0 = SIZE;
        int y0 = SIZE;
        int x1 = -1;
        int y1 = -1;
        boolean escaped = false;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[seed] = true;
        queue.add(seed);
        while (!queue.isEmpty()) {
            int p = queue.poll();
            int x = p % SIZE;
            int y = p / SIZE;
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            x1 = Math.max(x1, x);
            y1 = Math.max(y1, y);
            int[][] next = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
            for (int[] n : next) {
                if (n[0] < minX || n[0] > maxX || n[1] < minY || n[1] > maxY || !inTexture(n[0], n[1])) {
                    escaped = true;
                    continue;
                }
                int q = n[1] * SIZE + n[0];
                if (!seen[q] && !outline(n[0], n[1])) {
                    seen[q] = true;
                    queue.add(q);
                }
            }
        }
        return escaped ? null : new Rect(x0, y0 + LAYER_Y, x1 + 1, y1 + 1 + LAYER_Y);
    }

    /** Outline pixels: near-black, or (nearly) nothing drawn. */
    private boolean outline(int x, int y) {
        int color = composed[y * SIZE + x];
        return ARGB.alpha(color) < 128 || ARGB.red(color) < 40 && ARGB.green(color) < 40 && ARGB.blue(color) < 40;
    }

    private static boolean inTexture(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    /** Draws {@code pageLayers} over each other as the screen does, with straight alpha blending. */
    private void compose(List<Identifier> pageLayers) {
        int[] out = new int[SIZE * SIZE];
        for (Identifier id : pageLayers) {
            int[] layer = layers.computeIfAbsent(id, NavigatorBounds::read);
            for (int i = 0; i < out.length; i++) {
                int src = layer[i];
                float a = ARGB.alpha(src) / 255.0F;
                if (a <= 0.0F) {
                    continue;
                }
                int dst = out[i];
                float da = ARGB.alpha(dst) / 255.0F;
                float oa = a + da * (1.0F - a);
                out[i] = ARGB.color(Math.round(oa * 255.0F),
                        blend(ARGB.red(src), ARGB.red(dst), a, da, oa),
                        blend(ARGB.green(src), ARGB.green(dst), a, da, oa),
                        blend(ARGB.blue(src), ARGB.blue(dst), a, da, oa));
            }
        }
        composed = out;
        composedFor = List.copyOf(pageLayers);
    }

    private static int blend(int src, int dst, float a, float da, float oa) {
        return Math.round((src * a + dst * da * (1.0F - a)) / oa);
    }

    /** A layer's ARGB pixels, or a blank layer if it cannot be read. */
    private static int[] read(Identifier id) {
        int[] pixels = new int[SIZE * SIZE];
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return pixels;
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int width = Math.min(SIZE, image.getWidth());
            int height = Math.min(SIZE, image.getHeight());
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * SIZE + x] = image.getPixel(x, y);
                }
            }
        } catch (IOException exception) {
            Apeira.LOGGER.warn("Could not read navigator layer {}", id, exception);
        }
        return pixels;
    }

    static int slotX(int slot) {
        return 8 + slot % 9 * 18;
    }

    static int slotY(int slot) {
        return 18 + slot / 9 * 18;
    }
}
