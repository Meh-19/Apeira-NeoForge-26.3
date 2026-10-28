package com.apeira.decor.client.bedrock;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;

/**
 * Draws a {@link BedrockModel}, posed by a {@link BedrockAnimation}, as Bedrock and Blockbench show it: each bone moves by
 * its animated position (x mirrored), turns about its pivot by its rest rotation plus the animated one (x and y
 * mirrored, z, y, x order), then scales; each cube turns about its own pivot. The model's origin is the pose stack's
 * origin, a block is 16 pixels, and Blockbench's north (-z) is the model's front. Every cube is one custom geometry
 * submission with the caller's render type, so an emissive type ({@code EmissiveRenderTypes}) keeps alpha 254 texels lit.
 */
public final class BedrockRenderer {
    private BedrockRenderer() {}

    /**
     * Submits {@code model} posed at {@code seconds} into {@code animation} (its rest pose without one), tinted
     * {@code color} (ARGB).
     */
    public static void submit(BedrockModel model, @Nullable BedrockAnimation animation, float seconds, PoseStack poseStack,
            SubmitNodeCollector collector, RenderType renderType, int light, int overlay, int color) {
        float time = animation == null ? 0 : animation.timeAt(seconds);
        float[] scratch = new float[3];
        for (BedrockModel.Bone bone : model.bones()) {
            bone(bone, animation, time, scratch, poseStack, collector, renderType, light, overlay, color);
        }
    }

    private static void bone(BedrockModel.Bone bone, @Nullable BedrockAnimation animation, float time, float[] scratch, PoseStack poseStack,
            SubmitNodeCollector collector, RenderType renderType, int light, int overlay, int color) {
        BedrockAnimation.BoneTrack track = animation == null ? null : animation.bones().get(bone.name());
        poseStack.pushPose();
        float rotX = bone.rotX();
        float rotY = bone.rotY();
        float rotZ = bone.rotZ();
        float scaleX = 1;
        float scaleY = 1;
        float scaleZ = 1;
        if (track != null) {
            if (track.position() != null) {
                track.position().sample(time, scratch);
                poseStack.translate(-scratch[0] / 16.0F, scratch[1] / 16.0F, scratch[2] / 16.0F);
            }
            if (track.rotation() != null) {
                track.rotation().sample(time, scratch);
                rotX -= scratch[0] * Mth.DEG_TO_RAD;
                rotY -= scratch[1] * Mth.DEG_TO_RAD;
                rotZ += scratch[2] * Mth.DEG_TO_RAD;
            }
            if (track.scale() != null) {
                track.scale().sample(time, scratch);
                scaleX = scratch[0];
                scaleY = scratch[1];
                scaleZ = scratch[2];
            }
        }
        poseStack.translate(bone.pivotX() / 16.0F, bone.pivotY() / 16.0F, bone.pivotZ() / 16.0F);
        rotate(poseStack, rotX, rotY, rotZ);
        // A bone scaled to 0 (hidden) draws nothing, nor do its children.
        if (scaleX != 0 || scaleY != 0 || scaleZ != 0) {
            poseStack.scale(scaleX, scaleY, scaleZ);
            poseStack.translate(-bone.pivotX() / 16.0F, -bone.pivotY() / 16.0F, -bone.pivotZ() / 16.0F);
            for (BedrockModel.Cube cube : bone.cubes()) {
                cube(cube, poseStack, collector, renderType, light, overlay, color);
            }
            for (BedrockModel.Bone child : bone.children()) {
                bone(child, animation, time, scratch, poseStack, collector, renderType, light, overlay, color);
            }
        }
        poseStack.popPose();
    }

    private static void cube(BedrockModel.Cube cube, PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, int light,
            int overlay, int color) {
        if (cube.quads().isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(cube.pivotX() / 16.0F, cube.pivotY() / 16.0F, cube.pivotZ() / 16.0F);
        rotate(poseStack, cube.rotX(), cube.rotY(), cube.rotZ());
        poseStack.translate(-cube.pivotX() / 16.0F, -cube.pivotY() / 16.0F, -cube.pivotZ() / 16.0F);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (BedrockModel.Quad quad : cube.quads()) {
                for (float[] vertex : quad.vertices()) {
                    vertex(buffer, pose, vertex, quad, light, overlay, color);
                }
            }
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float[] vertex, BedrockModel.Quad quad, int light, int overlay,
            int color) {
        buffer.addVertex(pose, vertex[0], vertex[1], vertex[2]).setColor(color).setUv(vertex[3], vertex[4]).setOverlay(overlay)
                .setLight(light).setNormal(pose, quad.normalX(), quad.normalY(), quad.normalZ());
    }

    private static void rotate(PoseStack poseStack, float x, float y, float z) {
        if (x != 0 || y != 0 || z != 0) {
            poseStack.rotate(new Quaternionf().rotationZYX(z, y, x));
        }
    }
}
