package com.apeira.main.client.entity;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.entity.WraithEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws Xenon's wraith ({@code geckolib/models/entity/wraith.geo.json}, the animated
 * {@code textures/entity/wraith.png}) translucent and without a shadow, and not at all while it has vanished. Its head
 * follows where it looks, up to 45 degrees sideways and 30 up or down, on top of the animation.
 */
public class WraithRenderer extends GeoEntityRenderer<WraithEntity, LivingEntityRenderState> {
    private static final float TO_RAD = (float) Math.PI / 180F;

    private static final DataTicket<Boolean> VANISHED = DataTicket.create("apeira_wraith_vanished", Boolean.class);
    /** Head yaw relative to the body and head pitch, in degrees, interpolated as Xenon did. */
    private static final DataTicket<Float> HEAD_YAW = DataTicket.create("apeira_wraith_head_yaw", Float.class);
    private static final DataTicket<Float> HEAD_PITCH = DataTicket.create("apeira_wraith_head_pitch", Float.class);

    public WraithRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ApeiraIds.id("wraith")));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void addRenderData(WraithEntity wraith, Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(VANISHED, wraith.isWraithInvisible());
        float bodyYaw = Mth.rotLerp(partialTick, wraith.yBodyRotO, wraith.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, wraith.yHeadRotO, wraith.yHeadRot);
        renderState.addGeckolibData(HEAD_YAW, Mth.wrapDegrees(headYaw - bodyYaw));
        renderState.addGeckolibData(HEAD_PITCH, Mth.lerp(partialTick, wraith.xRotO, wraith.getXRot()));
    }

    @Override
    public void submit(LivingEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (renderState.getOrDefaultGeckolibData(VANISHED, false)) return;
        super.submit(renderState, poseStack, collector, cameraState);
    }

    /** Always translucent, for a ghostly look. */
    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        LivingEntityRenderState state = renderPassInfo.renderState();
        float relYaw = Mth.clamp(state.getOrDefaultGeckolibData(HEAD_YAW, 0.0F), -45.0F, 45.0F);
        float pitch = Mth.clamp(state.getOrDefaultGeckolibData(HEAD_PITCH, 0.0F), -30.0F, 30.0F);
        snapshots.ifPresent("head", head -> {
            head.setRotX(head.getRotX() + pitch * TO_RAD);
            head.setRotY(head.getRotY() + relYaw * TO_RAD);
        });
    }
}
