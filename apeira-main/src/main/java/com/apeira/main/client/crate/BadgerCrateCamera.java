package com.apeira.main.client.crate;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The spectator camera of the Badger Crate opening: the recorded player's eye from Ren's Blockbench file
 * ({@code assets/apeira/badger_crate/camera.json}, written by {@code tools/badger-crate-export}), one frame per tick,
 * relative to the animation's origin. Past the last frame the camera holds still.
 */
final class BadgerCrateCamera {
    private static final Identifier FILE = ApeiraIds.id("badger_crate/camera.json");
    private static List<double[]> frames = List.of();

    private BadgerCrateCamera() {}

    static void load() {
        List<double[]> loaded = new ArrayList<>();
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("frames")) {
                JsonArray frame = element.getAsJsonArray();
                double[] values = new double[5];
                for (int i = 0; i < 5; i++) {
                    values[i] = frame.get(i).getAsDouble();
                }
                loaded.add(values);
            }
        } catch (Exception e) {
            Apeira.LOGGER.warn("Could not read {}", FILE, e);
        }
        if (loaded.isEmpty()) {
            loaded.add(new double[] {-1.35, 1.12, 2.61, -154.7, 4.2});
        }
        frames = List.copyOf(loaded);
    }

    /** The eye at {@code ticks} into the animation, relative to its origin. */
    static Vec3 position(float ticks) {
        double[] a = frame((int) Math.floor(ticks));
        double[] b = frame((int) Math.floor(ticks) + 1);
        double t = ticks - Math.floor(ticks);
        return new Vec3(Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2]));
    }

    static float yaw(float ticks) {
        double[] a = frame((int) Math.floor(ticks));
        double[] b = frame((int) Math.floor(ticks) + 1);
        return (float) (a[3] + Mth.wrapDegrees(b[3] - a[3]) * (ticks - Math.floor(ticks)));
    }

    static float pitch(float ticks) {
        double[] a = frame((int) Math.floor(ticks));
        double[] b = frame((int) Math.floor(ticks) + 1);
        return (float) Mth.lerp(ticks - Math.floor(ticks), a[4], b[4]);
    }

    private static double[] frame(int index) {
        if (frames.isEmpty()) {
            load();
        }
        return frames.get(Mth.clamp(index, 0, frames.size() - 1));
    }
}
