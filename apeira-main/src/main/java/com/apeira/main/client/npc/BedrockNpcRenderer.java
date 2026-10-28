package com.apeira.main.client.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.bedrock.BedrockAnimation;
import com.apeira.decor.client.bedrock.BedrockAssets;
import com.apeira.decor.client.bedrock.BedrockModel;
import com.apeira.decor.client.bedrock.BedrockRenderer;
import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws an {@link NpcEntity} from a Bedrock model and animation ({@code assets/<namespace>/bedrock/}, loaded by decor's
 * {@link BedrockAssets}) through decor's {@link BedrockRenderer}, not GeckoLib: the model's origin at the entity's feet,
 * its front (Blockbench's north) turned to the entity's facing, the animation looping on the entity's age. It is drawn
 * with the emissive translucent entity type, so alpha 254 texels glow. A hidden NPC is not drawn at all until the
 * viewer comes within {@link #GHOST_REACH} block of it; then it fades in as a faint ghost ({@link #GHOST_ALPHA}) over
 * {@link #GHOST_FADE_MS}, and fades out again when they step away. A moved NPC (the anchor tool's teleport) is drawn at
 * its new spot at once.
 *
 * <p>The model's {@code chat_display} bone (the text display Origin Realms showed over the NPC) carries a speech
 * bubble, always shown: the first of the six 16x16 frames of {@code textures/custom/ui/speech_bubble.png}, a
 * {@link #BUBBLE_SIZE}-block billboard at that bone's animated pivot (through {@link BedrockRenderer.Attachment}),
 * always facing the camera, centred across with its bottom edge on the pivot. It is full bright: its texels are alpha 254
 * (emissive, section 5) and its vertices carry full-bright light, so it stays pure white in the dark.
 */
public class BedrockNpcRenderer extends EntityRenderer<NpcEntity, BedrockNpcRenderer.State> {
    /** The ghost's alpha, for a hidden NPC seen up close. */
    private static final int GHOST_ALPHA = 0x60;
    /** How near, in blocks between the viewer's and the NPC's bounding boxes, a hidden NPC shows as a ghost. */
    private static final double GHOST_REACH = 1.0;
    /** How long a hidden NPC's ghost takes to fade fully in or out. */
    private static final float GHOST_FADE_MS = 300.0F;
    /** The bone the speech bubble hangs on. */
    public static final String CHAT_BONE = "chat_display";
    private static final Identifier BUBBLE = Apeira.REGISTRAR.id("textures/custom/ui/speech_bubble.png");
    /** The bubble texture's frames, stacked top to bottom; the first is drawn. */
    private static final int BUBBLE_FRAMES = 6;
    /** The bubble's width and height, in blocks. */
    private static final float BUBBLE_SIZE = 0.5F;
    /** Each NPC's ghost fade. */
    private static final Map<NpcEntity, Track> TRACKS = new WeakHashMap<>();

    private static final class Track {
        /** The ghost's fade, 0 (gone) to 1 (at {@link #GHOST_ALPHA}), and when it was last moved. */
        float ghost;
        long ghostMillis = Util.getMillis();
    }

    /** Each NPC type's model, by type, for {@link #head}. */
    private static final Map<EntityType<?>, Rig> RIGS = new HashMap<>();

    /**
     * An NPC type's model.
     *
     * @param model      the model, {@code bedrock/models/<path>.geo.json} as {@code <namespace>:<path>}
     * @param animations the animation file, {@code bedrock/animations/<path>.animation.json} as {@code <namespace>:<path>}
     * @param animation  the looping animation's name in it
     * @param headBone   the bone the dialogue camera looks at ({@link #head}); a bone without cubes of its own is located by its
     *                   animated pivot, which is how a Blockbench locator exported as a bone is followed
     */
    public record Rig(Identifier model, Identifier animations, String animation, Identifier texture, String headBone) {}

    private final Rig rig;

    public BedrockNpcRenderer(EntityRendererProvider.Context context, Rig rig) {
        super(context);
        this.rig = rig;
        this.shadowRadius = 0.5F;
    }

    /** Registers {@code type}'s renderer, drawing {@code rig}. */
    public static void register(EntityRenderersEvent.RegisterRenderers event, EntityType<? extends NpcEntity> type, Rig rig) {
        RIGS.put(type, rig);
        event.registerEntityRenderer(type, context -> new BedrockNpcRenderer(context, rig));
    }

    /**
     * Where the NPC's head bone is drawn this frame, in world coordinates: the centre of the bone's cubes, posed by its
     * looping animation at the NPC's age as {@link #submit} poses it, and turned with the NPC. Its eyes when its model or
     * the bone is missing.
     */
    public static Vec3 head(NpcEntity npc, float partialTick) {
        Rig rig = RIGS.get(npc.getType());
        BedrockModel bedrock = rig == null ? null : BedrockAssets.model(rig.model());
        Vector3f local = bedrock == null ? null : BedrockRenderer.locate(bedrock, BedrockAssets.animation(rig.animations(), rig.animation()),
                (npc.tickCount + partialTick) / 20.0F, rig.headBone());
        if (local == null) {
            return npc.getEyePosition(partialTick);
        }
        local.rotateY((180.0F - npc.getViewYRot(partialTick)) * Mth.DEG_TO_RAD);
        return npc.getPosition(partialTick).add(local.x(), local.y(), local.z());
    }

    public static class State extends EntityRenderState {
        float yRot;
        boolean ghost;
        float ghostFade;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(NpcEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getViewYRot(partialTicks);
        state.ghost = !entity.isShown();
        Track ghostTrack = TRACKS.computeIfAbsent(entity, npc -> new Track());
        long now = Util.getMillis();
        Player viewer = Minecraft.getInstance().player;
        boolean near = state.ghost && viewer != null && viewer.getBoundingBox().distanceToSqr(entity.getBoundingBox()) <= GHOST_REACH * GHOST_REACH;
        float step = (now - ghostTrack.ghostMillis) / GHOST_FADE_MS;
        ghostTrack.ghost = Mth.clamp(ghostTrack.ghost + (near ? step : -step), 0.0F, 1.0F);
        ghostTrack.ghostMillis = now;
        state.ghostFade = ghostTrack.ghost;
        if (state.ghost && state.ghostFade <= 0.0F) {
            state.isInvisible = true;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BedrockModel bedrock = BedrockAssets.model(rig.model());
        if (bedrock != null && !state.isInvisible) {
            @Nullable BedrockAnimation idle = BedrockAssets.animation(rig.animations(), rig.animation());
            poseStack.pushPose();
            poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yRot));
            int color = state.ghost ? Math.round(GHOST_ALPHA * state.ghostFade) << 24 | 0xFFFFFF : 0xFFFFFFFF;
            int light = state.lightCoords;
            BedrockRenderer.submit(bedrock, idle, state.ageInTicks / 20.0F, poseStack, submitNodeCollector,
                    EmissiveRenderTypes.entityTranslucent(rig.texture()), light, OverlayTexture.NO_OVERLAY, color, (bone, pose, collector) -> {
                        if (CHAT_BONE.equals(bone)) {
                            bubble(pose, collector, camera.orientation, color);
                        }
                    });
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /**
     * The speech bubble's first frame: a square at the bone's pivot, turned to face the camera (as name tags are), its
     * bottom edge on the pivot, drawn from both sides.
     */
    private static void bubble(PoseStack poseStack, SubmitNodeCollector collector, Quaternionf facing, int color) {
        Vector3f anchor = poseStack.last().pose().getTranslation(new Vector3f());
        poseStack.pushPose();
        poseStack.last().pose().translation(anchor).rotate(facing);
        poseStack.last().normal().rotation(facing);
        float half = BUBBLE_SIZE / 2.0F;
        float v = 1.0F / BUBBLE_FRAMES;
        collector.submitCustomGeometry(poseStack, EmissiveRenderTypes.entityTranslucent(BUBBLE), (pose, buffer) -> {
            bubbleVertex(buffer, pose, -half, 0.0F, 0.0F, v, color);
            bubbleVertex(buffer, pose, half, 0.0F, 1.0F, v, color);
            bubbleVertex(buffer, pose, half, BUBBLE_SIZE, 1.0F, 0.0F, color);
            bubbleVertex(buffer, pose, -half, BUBBLE_SIZE, 0.0F, 0.0F, color);
        });
        poseStack.popPose();
    }

    private static void bubbleVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    /** The animation lifts and swings the model past its collision box and the bubble floats over it, so it is culled by a larger box. */
    @Override
    protected AABB getBoundingBoxForCulling(NpcEntity entity, float partialTicks) {
        return entity.getBoundingBox().inflate(1.0, 2.5, 1.0);
    }

    @Override
    protected float getShadowRadius(State state) {
        return state.isInvisible || state.ghost ? 0.0F : super.getShadowRadius(state);
    }
}
