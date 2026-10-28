package com.apeira.decor.client.bedrock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/**
 * One animation of a Bedrock {@code .animation.json} file, for {@link BedrockRenderer}: per bone, position, rotation
 * and scale keyframes, sampled linearly (a keyframe's {@code pre} and {@code post} values are kept apart, so a jump
 * stays a jump). Values are plain numbers; Molang expressions read as 0. Rotations are degrees, positions pixels, as
 * Bedrock writes them; {@link BedrockRenderer} mirrors them as it mirrors the geometry.
 *
 * @param length seconds
 * @param loop   whether it starts again after {@code length}; otherwise it holds its last frame
 */
public record BedrockAnimation(float length, boolean loop, Map<String, BoneTrack> bones) {
    /** A bone's channels; a missing channel leaves that part of the bone at rest. */
    public record BoneTrack(@Nullable Channel position, @Nullable Channel rotation, @Nullable Channel scale) {}

    /** Keyframes in time order: each frame's time, the value arriving at it and the value leaving it. */
    public record Channel(float[] times, float[][] pre, float[][] post) {
        /** The value at {@code time} seconds, into {@code out}. */
        public void sample(float time, float[] out) {
            int last = times.length - 1;
            if (time <= times[0]) {
                System.arraycopy(pre[0], 0, out, 0, 3);
                return;
            }
            if (time >= times[last]) {
                System.arraycopy(post[last], 0, out, 0, 3);
                return;
            }
            int next = 1;
            while (times[next] < time) {
                next++;
            }
            float span = times[next] - times[next - 1];
            float delta = span <= 0 ? 1 : (time - times[next - 1]) / span;
            for (int i = 0; i < 3; i++) {
                out[i] = Mth.lerp(delta, post[next - 1][i], pre[next][i]);
            }
        }
    }

    /** Every animation in an {@code .animation.json} file, by name. */
    public static Map<String, BedrockAnimation> parseAll(JsonObject json) {
        Map<String, BedrockAnimation> animations = new HashMap<>();
        JsonObject all = GsonHelper.getAsJsonObject(json, "animations", new JsonObject());
        for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
            animations.put(entry.getKey(), parse(entry.getValue().getAsJsonObject()));
        }
        return animations;
    }

    private static BedrockAnimation parse(JsonObject json) {
        JsonElement loop = json.get("loop");
        boolean loops = loop != null && loop.isJsonPrimitive() && (loop.getAsJsonPrimitive().isBoolean() ? loop.getAsBoolean()
                : "true".equals(loop.getAsString()));
        Map<String, BoneTrack> bones = new HashMap<>();
        float length = GsonHelper.getAsFloat(json, "animation_length", 0.0F);
        float lastFrame = 0;
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "bones", new JsonObject()).entrySet()) {
            JsonObject bone = entry.getValue().getAsJsonObject();
            BoneTrack track = new BoneTrack(channel(bone.get("position")), channel(bone.get("rotation")), channel(bone.get("scale")));
            for (Channel channel : new Channel[] {track.position, track.rotation, track.scale}) {
                if (channel != null) {
                    lastFrame = Math.max(lastFrame, channel.times[channel.times.length - 1]);
                }
            }
            bones.put(entry.getKey(), track);
        }
        return new BedrockAnimation(length > 0 ? length : lastFrame, loops, Map.copyOf(bones));
    }

    private static @Nullable Channel channel(@Nullable JsonElement json) {
        if (json == null) {
            return null;
        }
        if (!json.isJsonObject()) {
            float[] value = value(json);
            return new Channel(new float[] {0}, new float[][] {value}, new float[][] {value});
        }
        List<Map.Entry<Float, JsonElement>> frames = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            try {
                frames.add(Map.entry(Float.parseFloat(entry.getKey()), entry.getValue()));
            } catch (NumberFormatException ignored) {
                // Not a keyframe time.
            }
        }
        if (frames.isEmpty()) {
            return null;
        }
        frames.sort(Map.Entry.comparingByKey());
        float[] times = new float[frames.size()];
        float[][] pre = new float[frames.size()][];
        float[][] post = new float[frames.size()][];
        for (int i = 0; i < frames.size(); i++) {
            times[i] = frames.get(i).getKey();
            JsonElement frame = frames.get(i).getValue();
            if (frame.isJsonObject() && (frame.getAsJsonObject().has("pre") || frame.getAsJsonObject().has("post"))) {
                JsonObject object = frame.getAsJsonObject();
                pre[i] = value(object.has("pre") ? object.get("pre") : object.get("post"));
                post[i] = value(object.has("post") ? object.get("post") : object.get("pre"));
            } else {
                JsonElement vector = frame.isJsonObject() && frame.getAsJsonObject().has("vector") ? frame.getAsJsonObject().get("vector") : frame;
                pre[i] = post[i] = value(vector);
            }
        }
        return new Channel(times, pre, post);
    }

    /** A keyframe's value: three numbers, or one number for all three. */
    private static float[] value(JsonElement json) {
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            float[] value = new float[3];
            for (int i = 0; i < 3; i++) {
                value[i] = number(array.size() > i ? array.get(i) : array.get(0));
            }
            return value;
        }
        float value = number(json);
        return new float[] {value, value, value};
    }

    private static float number(JsonElement json) {
        try {
            return json.getAsFloat();
        } catch (RuntimeException notANumber) {
            return 0;
        }
    }

    /** The time into the animation {@code seconds} after it started: wrapped when it loops, held at the end when not. */
    public float timeAt(float seconds) {
        if (length <= 0) {
            return 0;
        }
        return loop ? seconds % length : Math.min(seconds, length);
    }
}
