package com.apeira.decor.client.bedrock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/**
 * Bedrock entity geometry ({@code .geo.json}, format 1.12 and up), baked for {@link BedrockRenderer}. The model is kept
 * as Bedrock describes it and drawn as Bedrock and Blockbench show it: x is mirrored (pivots and origins negated, x and y
 * rotations negated), cubes are boxes from their origin with per-face or box UVs, and bones turn about their pivot in
 * z, y, x order. Faces whose UV has no area and faces across a flat cube's zero side are left out.
 *
 * @param bones the top-level bones, each with its children
 */
public record BedrockModel(int textureWidth, int textureHeight, List<Bone> bones) {
    /**
     * A bone: its pivot in pixels (x already negated), its rest rotation in radians (z, y, x order) and its cubes and
     * children.
     */
    public record Bone(String name, float pivotX, float pivotY, float pivotZ, float rotX, float rotY, float rotZ, List<Cube> cubes,
            List<Bone> children) {}

    /** A cube: its own pivot in pixels and rotation in radians, and its faces in model units (a block is 1). */
    public record Cube(float pivotX, float pivotY, float pivotZ, float rotX, float rotY, float rotZ, List<Quad> quads) {}

    /** One face: four corners (x, y, z, u, v) and its normal. */
    public record Quad(float[][] vertices, float normalX, float normalY, float normalZ) {}

    /** Reads the first geometry of a {@code .geo.json} file. */
    public static BedrockModel parse(JsonObject json) {
        JsonArray geometries = GsonHelper.getAsJsonArray(json, "minecraft:geometry");
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = GsonHelper.getAsJsonObject(geometry, "description");
        int width = GsonHelper.getAsInt(description, "texture_width", 64);
        int height = GsonHelper.getAsInt(description, "texture_height", 64);
        Map<String, List<JsonObject>> children = new HashMap<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(geometry, "bones", new JsonArray())) {
            JsonObject bone = element.getAsJsonObject();
            String parent = GsonHelper.getAsString(bone, "parent", "");
            children.computeIfAbsent(parent, key -> new ArrayList<>()).add(bone);
        }
        return new BedrockModel(width, height, bake(children, "", width, height));
    }

    private static List<Bone> bake(Map<String, List<JsonObject>> children, String parent, int width, int height) {
        List<Bone> bones = new ArrayList<>();
        for (JsonObject json : children.getOrDefault(parent, List.of())) {
            String name = GsonHelper.getAsString(json, "name");
            float[] pivot = vec(json, "pivot");
            float[] rotation = vec(json, "rotation");
            boolean mirror = GsonHelper.getAsBoolean(json, "mirror", false);
            float inflate = GsonHelper.getAsFloat(json, "inflate", 0.0F);
            List<Cube> cubes = new ArrayList<>();
            for (JsonElement cube : GsonHelper.getAsJsonArray(json, "cubes", new JsonArray())) {
                cubes.add(cube(cube.getAsJsonObject(), mirror, inflate, width, height));
            }
            bones.add(new Bone(name, -pivot[0], pivot[1], pivot[2], -rotation[0] * Mth.DEG_TO_RAD, -rotation[1] * Mth.DEG_TO_RAD,
                    rotation[2] * Mth.DEG_TO_RAD, List.copyOf(cubes), bake(children, name, width, height)));
        }
        return List.copyOf(bones);
    }

    private static Cube cube(JsonObject json, boolean boneMirror, float boneInflate, int width, int height) {
        float[] origin = vec(json, "origin");
        float[] size = vec(json, "size");
        float[] pivot = vec(json, "pivot");
        float[] rotation = vec(json, "rotation");
        boolean mirror = GsonHelper.getAsBoolean(json, "mirror", boneMirror);
        float inflate = GsonHelper.getAsFloat(json, "inflate", boneInflate) / 16.0F;
        // Bedrock's x runs the other way: the box starts at -(x + width).
        float x0 = -(origin[0] + size[0]) / 16.0F - inflate;
        float y0 = origin[1] / 16.0F - inflate;
        float z0 = origin[2] / 16.0F - inflate;
        float x1 = x0 + size[0] / 16.0F + 2 * inflate;
        float y1 = y0 + size[1] / 16.0F + 2 * inflate;
        float z1 = z0 + size[2] / 16.0F + 2 * inflate;
        float[] bottomLeftBack = {x0, y0, z0};
        float[] bottomRightBack = {x0, y0, z1};
        float[] topLeftBack = {x0, y1, z0};
        float[] topRightBack = {x0, y1, z1};
        float[] topLeftFront = {x1, y1, z0};
        float[] topRightFront = {x1, y1, z1};
        float[] bottomLeftFront = {x1, y0, z0};
        float[] bottomRightFront = {x1, y0, z1};

        JsonElement uv = json.get("uv");
        boolean boxUv = uv != null && uv.isJsonArray();
        List<Quad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (isZeroSizeFace(size, direction)) {
                continue;
            }
            float[] faceUv = boxUv ? boxUv(direction, uv.getAsJsonArray(), size) : faceUv(uv, direction, size);
            if (faceUv == null || faceUv[2] == 0 && faceUv[3] == 0) {
                continue;
            }
            float[][] corners = switch (direction) {
                case WEST -> mirror ? new float[][] {topLeftFront, topRightFront, bottomRightFront, bottomLeftFront}
                        : new float[][] {topRightBack, topLeftBack, bottomLeftBack, bottomRightBack};
                case EAST -> mirror ? new float[][] {topRightBack, topLeftBack, bottomLeftBack, bottomRightBack}
                        : new float[][] {topLeftFront, topRightFront, bottomRightFront, bottomLeftFront};
                case NORTH -> new float[][] {topLeftBack, topLeftFront, bottomLeftFront, bottomLeftBack};
                case SOUTH -> new float[][] {topRightFront, topRightBack, bottomRightBack, bottomRightFront};
                case UP -> mirror && !boxUv ? new float[][] {bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack}
                        : new float[][] {topRightBack, topRightFront, topLeftFront, topLeftBack};
                case DOWN -> mirror && !boxUv ? new float[][] {topRightBack, topRightFront, topLeftFront, topLeftBack}
                        : new float[][] {bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack};
            };
            float u = faceUv[0];
            float v = faceUv[1];
            float uEnd = (u + faceUv[2]) / width;
            float vEnd = (v + faceUv[3]) / height;
            u /= width;
            v /= height;
            if (!mirror) {
                float swap = uEnd;
                uEnd = u;
                u = swap;
            }
            float[] uvs = switch (Math.floorMod((int) faceUv[4], 360) / 90) {
                case 1 -> new float[] {uEnd, v, uEnd, vEnd, u, vEnd, u, v};
                case 2 -> new float[] {uEnd, vEnd, u, vEnd, u, v, uEnd, v};
                case 3 -> new float[] {u, vEnd, u, v, uEnd, v, uEnd, vEnd};
                default -> new float[] {u, v, uEnd, v, uEnd, vEnd, u, vEnd};
            };
            float[][] vertices = new float[4][];
            for (int i = 0; i < 4; i++) {
                vertices[i] = new float[] {corners[i][0], corners[i][1], corners[i][2], uvs[i * 2], uvs[i * 2 + 1]};
            }
            quads.add(new Quad(vertices, mirror ? -direction.getStepX() : direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        }
        return new Cube(-pivot[0], pivot[1], pivot[2], -rotation[0] * Mth.DEG_TO_RAD, -rotation[1] * Mth.DEG_TO_RAD,
                rotation[2] * Mth.DEG_TO_RAD, List.copyOf(quads));
    }

    private static boolean isZeroSizeFace(float[] size, Direction direction) {
        if (size[0] == 0) {
            return direction.getAxis() != Direction.Axis.X;
        }
        if (size[1] == 0) {
            return direction.getAxis() != Direction.Axis.Y;
        }
        if (size[2] == 0) {
            return direction.getAxis() != Direction.Axis.Z;
        }
        return false;
    }

    /** A face's u, v, width, height and UV rotation in degrees, from per-face UVs; null when the face has none. */
    private static float @Nullable [] faceUv(@Nullable JsonElement uv, Direction direction, float[] size) {
        if (uv == null || !uv.isJsonObject() || !uv.getAsJsonObject().has(direction.getSerializedName())) {
            return null;
        }
        JsonObject face = uv.getAsJsonObject().getAsJsonObject(direction.getSerializedName());
        float[] start = vec(face, "uv");
        float[] faceSize = face.has("uv_size") ? vec(face, "uv_size") : switch (direction.getAxis()) {
            case X -> new float[] {size[2], size[1]};
            case Y -> new float[] {size[0], size[2]};
            case Z -> new float[] {size[0], size[1]};
        };
        return new float[] {start[0], start[1], faceSize[0], faceSize[1], GsonHelper.getAsInt(face, "uv_rotation", 0)};
    }

    /** A face's place in a box UV layout starting at {@code uv}. */
    private static float[] boxUv(Direction direction, JsonArray uv, float[] size) {
        float u = uv.get(0).getAsFloat();
        float v = uv.get(1).getAsFloat();
        float x = size[0];
        float y = size[1];
        float z = size[2];
        return switch (direction) {
            case WEST -> new float[] {u + z + x, v + z, z, y, 0};
            case EAST -> new float[] {u, v + z, z, y, 0};
            case NORTH -> new float[] {u + z, v + z, x, y, 0};
            case SOUTH -> new float[] {u + z + x + z, v + z, x, y, 0};
            case UP -> new float[] {u + z, v, x, z, 0};
            case DOWN -> new float[] {u + z + x, v + z, x, -z, 0};
        };
    }

    /** A three-number array (a vector or a UV pair padded with 0), 0, 0, 0 when it is missing. */
    static float[] vec(JsonObject json, String key) {
        float[] result = new float[3];
        JsonElement element = json.get(key);
        if (element != null && element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < Math.min(3, array.size()); i++) {
                result[i] = array.get(i).getAsFloat();
            }
        }
        return result;
    }
}
