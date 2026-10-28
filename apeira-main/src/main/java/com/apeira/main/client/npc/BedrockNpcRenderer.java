package com.apeira.main.client.npc;

import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.PlacementGlide;
import com.apeira.decor.client.bedrock.BedrockAnimation;
import com.apeira.decor.client.bedrock.BedrockAssets;
import com.apeira.decor.client.bedrock.BedrockModel;
import com.apeira.decor.client.bedrock.BedrockRenderer;
import com.apeira.main.npc.NpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws an {@link NpcEntity} from a Bedrock model and animation ({@code assets/<namespace>/bedrock/}, loaded by decor's
 * {@link BedrockAssets}) through decor's {@link BedrockRenderer}, not GeckoLib: the model's origin at the entity's feet,
 * its front (Blockbench's north) turned to the entity's facing, the animation looping on the entity's age. It is drawn
 * with the emissive translucent entity type, so alpha 254 texels glow. A hidden NPC is drawn as a faint ghost for a
 * player who may edit it ({@link NpcClient#canEdit}) and not at all for anyone else. When the NPC jumps to a new spot
 * (the anchor tool's teleport), it glides there from where it was drawn ({@link PlacementGlide}) instead of snapping.
 */
public class BedrockNpcRenderer extends EntityRenderer<NpcEntity, BedrockNpcRenderer.State> {
    /** The ghost's alpha, for a hidden NPC seen by one who may edit it. */
    private static final int GHOST_ALPHA = 0x60;
    /** A move longer than this between two frames is a teleport, which glides; shorter ones are drawn as they are. */
    private static final double JUMP = 0.5;

    /** Each NPC's last drawn position and yaw, and its glide while one runs. */
    private static final Map<NpcEntity, Track> TRACKS = new WeakHashMap<>();

    private static final class Track {
        Vec3 position;
        float yaw;
        @Nullable PlacementGlide glide;

        Track(Vec3 position, float yaw) {
            this.position = position;
            this.yaw = yaw;
        }
    }

    private final Identifier model;
    private final Identifier animations;
    private final String animation;
    private final Identifier texture;

    /**
     * @param model      the model, {@code bedrock/models/<path>.geo.json} as {@code <namespace>:<path>}
     * @param animations the animation file, {@code bedrock/animations/<path>.animation.json} as {@code <namespace>:<path>}
     * @param animation  the looping animation's name in it
     */
    public BedrockNpcRenderer(EntityRendererProvider.Context context, Identifier model, Identifier animations, String animation,
            Identifier texture) {
        super(context);
        this.model = model;
        this.animations = animations;
        this.animation = animation;
        this.texture = texture;
        this.shadowRadius = 0.5F;
    }

    public static class State extends EntityRenderState {
        float yRot;
        boolean ghost;
        Vec3 glide = Vec3.ZERO;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(NpcEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getViewYRot(partialTicks);
        Vec3 position = entity.getPosition(partialTicks);
        Track track = TRACKS.get(entity);
        if (track == null) {
            TRACKS.put(entity, new Track(position, state.yRot));
        } else {
            if (track.position.distanceToSqr(position) > JUMP * JUMP) {
                // Glide from where it was last drawn, mid-glide included.
                Vec3 drawn = track.glide != null && !track.glide.done() ? track.position.add(track.glide.offset()) : track.position;
                float drawnYaw = track.glide != null && !track.glide.done() ? track.glide.yaw(track.yaw) : track.yaw;
                track.glide = PlacementGlide.between(drawn, position, drawnYaw);
            }
            track.position = position;
            track.yaw = state.yRot;
            if (track.glide != null) {
                if (track.glide.done()) {
                    track.glide = null;
                } else {
                    state.glide = track.glide.offset();
                    state.yRot = track.glide.yaw(state.yRot);
                }
            }
        }
        state.ghost = !entity.isShown();
        if (state.ghost && !NpcClient.canEdit(entity)) {
            state.isInvisible = true;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BedrockModel bedrock = BedrockAssets.model(model);
        if (bedrock != null && !state.isInvisible) {
            @Nullable BedrockAnimation idle = BedrockAssets.animation(animations, animation);
            poseStack.pushPose();
            poseStack.translate(state.glide.x, state.glide.y, state.glide.z);
            poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yRot));
            int color = state.ghost ? GHOST_ALPHA << 24 | 0xFFFFFF : 0xFFFFFFFF;
            BedrockRenderer.submit(bedrock, idle, state.ageInTicks / 20.0F, poseStack, submitNodeCollector,
                    EmissiveRenderTypes.entityTranslucent(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, color);
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /** The animation lifts and swings the model past its collision box, so it is culled by a larger box. */
    @Override
    protected AABB getBoundingBoxForCulling(NpcEntity entity, float partialTicks) {
        AABB box = entity.getBoundingBox().inflate(1.0, 1.5, 1.0);
        Track track = TRACKS.get(entity);
        // While gliding it is drawn between its old and new spots.
        return track != null && track.glide != null ? box.minmax(box.move(track.glide.from().subtract(track.glide.to()))) : box;
    }

    @Override
    protected float getShadowRadius(State state) {
        return state.isInvisible || state.ghost ? 0.0F : super.getShadowRadius(state);
    }
}
