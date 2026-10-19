package com.apeira.main.client.entity;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.entity.TurkeyEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Draws the turkey's GeckoLib model ({@code geckolib/models/entity/turkey.geo.json}, {@code textures/entity/turkey.png},
 * {@code geckolib/animations/entity/turkey.animation.json}). Chicks are drawn at 0.6 size with a 1.5 times larger head,
 * as in Xenon.
 */
public class TurkeyRenderer extends GeoEntityRenderer<TurkeyEntity, LivingEntityRenderState> {
    private static final float BABY_SCALE = 0.6F;
    private static final float BABY_HEAD_SCALE = 1.5F;

    public TurkeyRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ApeiraIds.id("turkey")));
        this.shadowRadius = 0.3F;
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
        float baby = renderPassInfo.renderState().isBaby ? BABY_SCALE : 1.0F;
        super.scaleModelForRender(renderPassInfo, widthScale * baby, heightScale * baby);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        if (renderPassInfo.renderState().isBaby) {
            snapshots.ifPresent("head", head -> head.setScale(BABY_HEAD_SCALE, BABY_HEAD_SCALE, BABY_HEAD_SCALE));
        }
    }
}
