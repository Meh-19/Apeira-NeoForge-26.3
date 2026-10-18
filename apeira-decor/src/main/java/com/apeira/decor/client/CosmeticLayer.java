package com.apeira.decor.client;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.decor.cosmetic.Wardrobe;
import com.apeira.decor.registry.DecorAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;

/**
 * Draws the cosmetics in a player's (or mannequin's) wardrobe: the hat on the head, the back cosmetic on the body
 * and the hand cosmetic in the off hand while it holds nothing. Transforms are CosmoLib's {@code CosmeticPlayerLayer}.
 * Models are resolved under {@link WornCosmetic}, so item definitions draw the cosmetic's 3D model here and its token
 * everywhere else.
 */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** CosmoLib's {@code BackOffsetData.DEFAULT_Y}: lifts back cosmetics from the body's pivot onto the back. */
    private static final float BACK_Y = -0.57625F;
    private static final ContextKey<Worn> WORN = new ContextKey<>(ApeiraIds.id("cosmetics"));

    /** The resolved models of the worn cosmetics, collected while extracting the render state. */
    private record Worn(ItemStackRenderState hat, ItemStackRenderState back, ItemStackRenderState hand) {}

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** Resolves the wardrobe's item models into the render state; registered for every avatar renderer. */
    public static final AvatarRenderStateModifier MODIFIER = new AvatarRenderStateModifier() {
        @Override
        public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
            Wardrobe wardrobe = avatar.getData(DecorAttachments.WARDROBE.get());
            if (wardrobe.isEmpty()) {
                state.setRenderData(WORN, null);
                return;
            }
            ItemModelResolver resolver = Minecraft.getInstance().getItemModelResolver();
            HumanoidArm offArm = avatar.getMainArm().getOpposite();
            state.setRenderData(WORN, new Worn(
                    resolve(resolver, avatar, wardrobe.get(CosmeticSlot.HAT), ItemDisplayContext.HEAD),
                    resolve(resolver, avatar, wardrobe.get(CosmeticSlot.BACK), ItemDisplayContext.HEAD),
                    resolve(resolver, avatar, wardrobe.get(CosmeticSlot.HAND), offArm == HumanoidArm.LEFT
                            ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                            : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)));
        }
    };

    private static ItemStackRenderState resolve(ItemModelResolver resolver, Avatar avatar, ItemStack stack,
            ItemDisplayContext context) {
        ItemStackRenderState state = new ItemStackRenderState();
        WornCosmetic.whileWorn(() -> resolver.updateForLiving(state, stack, context, avatar));
        return state;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state,
            float yRot, float xRot) {
        Worn worn = state.getRenderData(WORN);
        if (worn == null || state.isInvisible) {
            return;
        }
        PlayerModel model = getParentModel();
        if (!worn.hat().isEmpty()) {
            poseStack.pushPose();
            model.root().translateAndRotate(poseStack);
            model.head.translateAndRotate(poseStack);
            CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
            worn.hat().submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        if (!worn.back().isEmpty()) {
            poseStack.pushPose();
            model.root().translateAndRotate(poseStack);
            model.body.translateAndRotate(poseStack);
            poseStack.translate(0.0F, BACK_Y, 0.0F);
            poseStack.rotateDegrees(Axis.YP, 180.0F);
            poseStack.scale(0.625F, -0.625F, -0.625F);
            worn.back().submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        HumanoidArm offArm = state.mainArm.getOpposite();
        if (!worn.hand().isEmpty() && state.getUseItemStackForArm(offArm).isEmpty()) {
            boolean left = offArm == HumanoidArm.LEFT;
            ModelPart arm = left ? model.leftArm : model.rightArm;
            poseStack.pushPose();
            model.root().translateAndRotate(poseStack);
            arm.translateAndRotate(poseStack);
            poseStack.translate(left ? 0.03125F : -0.03125F, 0.625F, -0.125F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            worn.hand().submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
