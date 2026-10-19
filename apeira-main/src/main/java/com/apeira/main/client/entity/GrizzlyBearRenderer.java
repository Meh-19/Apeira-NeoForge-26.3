package com.apeira.main.client.entity;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.entity.GrizzlyBearEntity;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Draws the grizzly bear's GeckoLib model ({@code geckolib/models/entity/grizzly_bear.geo.json},
 * {@code textures/entity/grizzly_bear.png}, {@code geckolib/animations/entity/grizzly_bear.animation.json}) with the
 * item in its mouth ({@link GrizzlyBearItemLayer}). As Xenon's model did, it shows one face overlay
 * ({@link GrizzlyBearEntity#getFaceState}), the honey overlays while the bear is honey distracted and the Yogi hat
 * and tie when it is named "Yogi", and tilts the head down while it fishes. Cubs are drawn at half size with a 1.6
 * times larger head.
 */
public class GrizzlyBearRenderer extends GeoEntityRenderer<GrizzlyBearEntity, LivingEntityRenderState> {
    private static final float BABY_SCALE = 0.5F;
    private static final float BABY_HEAD_SCALE = 1.6F;
    private static final float FISHING_HEAD_PITCH = -25.0F * ((float) Math.PI / 180.0F);

    private static final String[] HONEY_BONES = {"top_mouth_honey", "bottom_jaw_honey", "drip_honey", "right_paw_honey",
            "left_paw_honey"};
    private static final String[] YOGI_BONES = {"yogi_hat", "yogi_body"};

    private static final DataTicket<Integer> FACE = DataTicket.create("apeira_grizzly_face", Integer.class);
    private static final DataTicket<Boolean> HONEY = DataTicket.create("apeira_grizzly_honey", Boolean.class);
    private static final DataTicket<Boolean> YOGI = DataTicket.create("apeira_grizzly_yogi", Boolean.class);
    private static final DataTicket<Boolean> FISHING = DataTicket.create("apeira_grizzly_fishing", Boolean.class);

    public GrizzlyBearRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ApeiraIds.id("grizzly_bear")));
        this.shadowRadius = 0.7F;
        withRenderLayer(new GrizzlyBearItemLayer(this, context.getItemModelResolver()));
    }

    @Override
    public void addRenderData(GrizzlyBearEntity bear, @Nullable Void relatedObject, LivingEntityRenderState renderState,
            float partialTick) {
        super.addRenderData(bear, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(FACE, bear.getFaceState());
        renderState.addGeckolibData(HONEY, bear.isHoneyDistracted());
        renderState.addGeckolibData(YOGI, bear.hasCustomName() && "Yogi".equals(bear.getName().getString()));
        renderState.addGeckolibData(FISHING, bear.isFishing() && !bear.isCarrying());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
        float baby = renderPassInfo.renderState().isBaby ? BABY_SCALE : 1.0F;
        super.scaleModelForRender(renderPassInfo, widthScale * baby, heightScale * baby);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        LivingEntityRenderState state = renderPassInfo.renderState();

        int face = state.getOrDefaultGeckolibData(FACE, GrizzlyBearEntity.FACE_ANGRY);
        show(snapshots, "face_angry", face == GrizzlyBearEntity.FACE_ANGRY);
        show(snapshots, "face_passive", face == GrizzlyBearEntity.FACE_PASSIVE);
        show(snapshots, "face_sleeping", face == GrizzlyBearEntity.FACE_SLEEPING);

        boolean honey = state.getOrDefaultGeckolibData(HONEY, false);
        for (String bone : HONEY_BONES) {
            show(snapshots, bone, honey);
        }

        boolean yogi = state.getOrDefaultGeckolibData(YOGI, false);
        for (String bone : YOGI_BONES) {
            show(snapshots, bone, yogi);
        }

        // Fishing: the head looks down into the water.
        if (state.getOrDefaultGeckolibData(FISHING, false)) {
            snapshots.ifPresent("head", head -> head.setRotX(FISHING_HEAD_PITCH).setRotY(0.0F));
        }

        if (state.isBaby) {
            snapshots.ifPresent("head", head -> head.setScale(BABY_HEAD_SCALE, BABY_HEAD_SCALE, BABY_HEAD_SCALE));
        }
    }

    /** Shows or hides a bone by scaling it to 1 or 0, as Xenon did. */
    private static void show(BoneSnapshots snapshots, String bone, boolean visible) {
        float scale = visible ? 1.0F : 0.0F;
        snapshots.ifPresent(bone, (BoneSnapshot snapshot) -> snapshot.setScale(scale, scale, scale));
    }
}
