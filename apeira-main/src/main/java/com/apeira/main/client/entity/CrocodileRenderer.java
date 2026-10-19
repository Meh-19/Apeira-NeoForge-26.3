package com.apeira.main.client.entity;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.entity.CrocodileEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws Xenon's crocodile ({@code geckolib/models/entity/crocodile.geo.json}, {@code textures/entity/crocodile.png}).
 * A crocodile named "Pinky" is pink. On land its head follows where it looks (70%, up to 50 degrees sideways and 30 up
 * or down), and its jaw is open while it has a target. Babies are half size with a 1.4 times head.
 */
public class CrocodileRenderer extends GeoEntityRenderer<CrocodileEntity, LivingEntityRenderState> {
    private static final Identifier TEXTURE_PINKY = ApeiraIds.id("textures/entity/crocodile_pink.png");
    private static final float TO_RAD = (float) Math.PI / 180F;

    private static final DataTicket<Boolean> PINKY = DataTicket.create("apeira_crocodile_pinky", Boolean.class);
    private static final DataTicket<Boolean> IN_WATER = DataTicket.create("apeira_crocodile_in_water", Boolean.class);
    private static final DataTicket<Boolean> ATTACKING = DataTicket.create("apeira_crocodile_attacking", Boolean.class);
    private static final DataTicket<Boolean> AGGROED = DataTicket.create("apeira_crocodile_aggroed", Boolean.class);
    /** Head yaw relative to the body and head pitch, in degrees, interpolated as Xenon did. */
    private static final DataTicket<Float> HEAD_YAW = DataTicket.create("apeira_crocodile_head_yaw", Float.class);
    private static final DataTicket<Float> HEAD_PITCH = DataTicket.create("apeira_crocodile_head_pitch", Float.class);

    public CrocodileRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ApeiraIds.id("crocodile")));
        this.shadowRadius = 0.6F;
    }

    @Override
    public void addRenderData(CrocodileEntity croc, Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(PINKY, croc.hasCustomName() && "Pinky".equals(croc.getName().getString()));
        renderState.addGeckolibData(IN_WATER, croc.isInWater());
        renderState.addGeckolibData(ATTACKING, croc.isAttackingAnim());
        renderState.addGeckolibData(AGGROED, croc.isAggroed());
        float headYaw = Mth.rotLerp(partialTick, croc.yHeadRotO, croc.yHeadRot);
        float bodyYaw = Mth.rotLerp(partialTick, croc.yBodyRotO, croc.yBodyRot);
        renderState.addGeckolibData(HEAD_YAW, Mth.wrapDegrees(headYaw - bodyYaw));
        renderState.addGeckolibData(HEAD_PITCH, Mth.lerp(partialTick, croc.xRotO, croc.getXRot()));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState renderState) {
        return flag(renderState, PINKY) ? TEXTURE_PINKY : super.getTextureLocation(renderState);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
        float baby = renderPassInfo.renderState().isBaby ? 0.5F : 1.0F;
        super.scaleModelForRender(renderPassInfo, widthScale * baby, heightScale * baby);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        LivingEntityRenderState state = renderPassInfo.renderState();
        boolean attacking = flag(state, ATTACKING);

        if (!flag(state, IN_WATER) && !attacking) {
            float relYaw = Mth.clamp(state.getOrDefaultGeckolibData(HEAD_YAW, 0.0F), -50.0F, 50.0F);
            float pitch = Mth.clamp(state.getOrDefaultGeckolibData(HEAD_PITCH, 0.0F), -30.0F, 30.0F);
            snapshots.ifPresent("head", head -> {
                head.setRotY(head.getRotY() - relYaw * 0.7F * TO_RAD);
                head.setRotX(head.getRotX() - pitch * 0.7F * TO_RAD);
            });
        }

        if (flag(state, AGGROED) && !attacking) {
            snapshots.ifPresent("top_head", jaw -> jaw.setRotX(35.0F * TO_RAD));
        }

        if (state.isBaby) {
            snapshots.ifPresent("head", head -> head.setScale(1.4F, 1.4F, 1.4F));
        }
    }

    private static boolean flag(LivingEntityRenderState state, DataTicket<Boolean> ticket) {
        return state.getOrDefaultGeckolibData(ticket, false);
    }
}
