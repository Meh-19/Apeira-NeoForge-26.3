package com.apeira.main.client.npc;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
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

/**
 * Draws an {@link NpcEntity} from a Bedrock model and animation ({@code assets/<namespace>/bedrock/}, loaded by decor's
 * {@link BedrockAssets}) through decor's {@link BedrockRenderer}, not GeckoLib: the model's origin at the entity's feet,
 * its front (Blockbench's north) turned to the entity's facing, the animation looping on the entity's age. It is drawn
 * with the emissive translucent entity type, so alpha 254 texels glow. A hidden NPC is drawn as a faint ghost for a
 * player who may edit it ({@link NpcClient#canEdit}) and not at all for anyone else.
 */
public class BedrockNpcRenderer extends EntityRenderer<NpcEntity, BedrockNpcRenderer.State> {
    /** The ghost's alpha, for a hidden NPC seen by one who may edit it. */
    private static final int GHOST_ALPHA = 0x60;

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
        return entity.getBoundingBox().inflate(1.0, 1.5, 1.0);
    }

    @Override
    protected float getShadowRadius(State state) {
        return state.isInvisible || state.ghost ? 0.0F : super.getShadowRadius(state);
    }
}
