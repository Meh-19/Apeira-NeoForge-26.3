package com.apeira.uicapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * One item display's transform chain as text, for {@code /apeiradev dumparea} and each export: the entity, its item, custom
 * model data and display context, the transformation the server set (translation, raw quaternions, scale), each model's
 * {@code display.<context>} transform, and the rest pose the exporter bakes into each bone (pivot = parent pivot + rest
 * translation, rotation = rest rotation; the first frame's animation deltas are 0). The check line draws the item's centre
 * and its top corner both ways, through vanilla's render chain and through the exported bones read back the way GeckoLib
 * and Blockbench read them (pivot, rotation and scale per bone, cubes at their baked bind position), in blocks from the
 * area's bottom centre; a non-zero delta means the export does not match the game.
 */
final class DisplayDiagnostics {
    private DisplayDiagnostics() {
    }

    static List<String> describe(String bone, int entityId, Vec3 pos, Vec3 origin, float yaw, float pitch, @Nullable JsonObject item,
            String context, AreaEntities.@Nullable Transform tr, List<ItemModels.Model> models) {
        List<String> lines = new ArrayList<>();
        lines.add(bone + " (entity " + entityId + "): " + (item == null ? "no item" : AreaEntities.itemLabel(item))
                + (item != null && item.has("custom_model_data") ? " cmd=" + item.get("custom_model_data") : "") + " context=" + context);
        if (tr == null) {
            lines.add("  no transformation");
            return lines;
        }
        lines.add("  translation=" + vec(tr.translation()) + " left=" + quat(tr.left()) + " scale=" + vec(tr.scale()) + " right=" + quat(tr.right())
                + " yaw=" + num(yaw) + " pitch=" + num(pitch));
        double[] rootPos = ArmorStandExporter.position(pos, origin), rootRot = ArmorStandExporter.billboard(yaw, pitch);
        double[] trPos = ArmorStandExporter.translation(tr), trRot = ArmorStandExporter.euler(tr.left());
        double[] trScale = {tr.scale().x(), tr.scale().y(), tr.scale().z()}, rightRot = ArmorStandExporter.euler(tr.right());
        double[] trPivot = add(rootPos, trPos);
        lines.add("  bones (rest pose): " + bone + " pivot=" + arr(rootPos) + " rot=" + arr(rootRot) + "; _transform pivot=" + arr(trPivot)
                + " rot=" + arr(trRot) + " scale=" + arr(trScale) + "; _transform_right pivot=" + arr(trPivot) + " rot=" + arr(rightRot));
        if (models.isEmpty()) {
            lines.add("  no models resolved");
        }
        for (int m = 0; m < models.size(); m++) {
            ItemModels.Model model = models.get(m);
            double[][] raw = ItemGeometry.rawDisplay(model, context), bones = ItemGeometry.displayTransform(model, context);
            lines.add("  model " + m + " " + model.id() + " display." + context.toLowerCase(Locale.ROOT) + " t=" + arr(raw[0]) + " r=" + arr(raw[1])
                    + " s=" + arr(raw[2]) + " -> model bone pivot=" + arr(add(trPivot, bones[0])) + " rot=" + arr(bones[1]) + " scale=" + arr(bones[2]));

            Matrix4f game = new Matrix4f().translation((float) (pos.x - origin.x), (float) (pos.y - origin.y), (float) (pos.z - origin.z))
                    .rotate(new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0))
                    .translate(tr.translation()).rotate(tr.left()).scale(tr.scale()).rotate(tr.right())
                    .rotateY((float) Math.PI)
                    .translate((float) raw[0][0] / 16, (float) raw[0][1] / 16, (float) raw[0][2] / 16)
                    .rotate(new Quaternionf().rotationXYZ((float) Math.toRadians(raw[1][0]), (float) Math.toRadians(raw[1][1]), (float) Math.toRadians(raw[1][2])))
                    .scale((float) raw[2][0], (float) raw[2][1], (float) raw[2][2])
                    .translate(-0.5f, -0.5f, -0.5f);
            double[] modelPivot = add(trPivot, bones[0]), one = {1, 1, 1};
            Matrix4f exported = bone(rootPos, rootRot, one).mul(bone(trPivot, trRot, trScale)).mul(bone(trPivot, rightRot, one))
                    .mul(bone(modelPivot, bones[1], bones[2]));
            // Exported cubes are the model moved by -8 pixels and then to the model bone's pivot: model point p (blocks) is
            // pivot + p - 0.5 in bind space.
            Vector3f pivot = new Vector3f((float) -modelPivot[0] / 16, (float) modelPivot[1] / 16, (float) modelPivot[2] / 16);
            Vector3f centreGame = game.transformPosition(new Vector3f(0.5f, 0.5f, 0.5f)), cornerGame = game.transformPosition(new Vector3f(1, 1, 1));
            Vector3f centreBones = exported.transformPosition(new Vector3f(pivot)), cornerBones = exported.transformPosition(new Vector3f(pivot).add(0.5f, 0.5f, 0.5f));
            float delta = Math.max(centreGame.distance(centreBones), cornerGame.distance(cornerBones));
            lines.add("  check model " + m + ": centre game=" + vec(centreGame) + " bones=" + vec(centreBones) + ", corner game=" + vec(cornerGame)
                    + " bones=" + vec(cornerBones) + ", delta " + String.format(Locale.ROOT, "%.4f", delta * 16) + " px");
        }
        return lines;
    }

    /**
     * A Bedrock bone at rest (pivot in pixels, x mirrored; x and y rotations negated, turned z then y then x) as a Java
     * matrix in blocks: it turns and scales about its pivot.
     */
    private static Matrix4f bone(double[] pivot, double[] rotation, double[] scale) {
        float x = (float) -pivot[0] / 16, y = (float) pivot[1] / 16, z = (float) pivot[2] / 16;
        return new Matrix4f().translation(x, y, z)
                .rotateZYX((float) Math.toRadians(rotation[2]), (float) Math.toRadians(-rotation[1]), (float) Math.toRadians(-rotation[0]))
                .scale((float) scale[0], (float) scale[1], (float) scale[2]).translate(-x, -y, -z);
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static String num(double value) {
        String text = String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.equals("-0") ? "0" : text;
    }

    private static String arr(double[] values) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            text.append(i == 0 ? "" : ", ").append(num(values[i]));
        }
        return text.append(']').toString();
    }

    private static String vec(org.joml.Vector3fc v) {
        return arr(new double[] {v.x(), v.y(), v.z()});
    }

    private static String quat(Quaternionfc q) {
        return arr(new double[] {q.x(), q.y(), q.z(), q.w()});
    }
}
