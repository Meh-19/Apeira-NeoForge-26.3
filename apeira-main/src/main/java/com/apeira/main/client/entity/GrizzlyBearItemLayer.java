package com.apeira.main.client.entity;

import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import com.apeira.main.entity.GrizzlyBearEntity;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Draws the honeycomb or salmon in a grizzly bear's mouth on its {@code bottom_jaw} bone, lying flat and sticking out
 * forward, in the ground display. Ported from Xenon's {@code GrizzlyBearItemLayer}, with its offset: the pose at the
 * bone is the model's origin moved with the bone, so the offset reaches the jaw's pivot (-4, 13, -33 pixels).
 */
final class GrizzlyBearItemLayer extends GeoRenderLayer<GrizzlyBearEntity, Void, LivingEntityRenderState> {
    private static final String BONE_NAME = "bottom_jaw";
    private static final DataTicket<ItemStackRenderState> MOUTH_ITEM = DataTicket.create("apeira_grizzly_mouth_item",
            ItemStackRenderState.class);

    private final ItemModelResolver itemModelResolver;

    GrizzlyBearItemLayer(GeoRenderer<GrizzlyBearEntity, Void, LivingEntityRenderState> renderer, ItemModelResolver itemModelResolver) {
        super(renderer);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void addRenderData(GrizzlyBearEntity bear, @Nullable Void relatedObject, LivingEntityRenderState renderState,
            float partialTick) {
        ItemStack stack = switch (bear.getMouthItemType()) {
            case GrizzlyBearEntity.MOUTH_HONEYCOMB -> new ItemStack(Items.HONEYCOMB);
            case GrizzlyBearEntity.MOUTH_SALMON -> new ItemStack(Items.SALMON);
            default -> ItemStack.EMPTY;
        };
        // Render states are reused between frames, so the item is always set, empty when there is none.
        ItemStackRenderState item = new ItemStackRenderState();
        if (!stack.isEmpty()) {
            itemModelResolver.updateForLiving(item, stack, ItemDisplayContext.GROUND, bear);
        }
        renderState.addGeckolibData(MOUTH_ITEM, item);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo,
            BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        if (!renderPassInfo.willRender()) {
            return;
        }
        ItemStackRenderState item = renderPassInfo.getGeckolibData(MOUTH_ITEM);
        if (item == null || item.isEmpty()) {
            return;
        }
        renderPassInfo.model().getBone(BONE_NAME).ifPresent(bone -> consumer.accept(bone, (info, renderBone, tasks) -> {
            PoseStack poseStack = info.poseStack();
            poseStack.pushPose();
            // Xenon's offset in blocks: + right/up/back, - left/down/forward (out of the mouth).
            poseStack.translate(0.0F, 0.828125F, -2.0F);
            // Lying flat, sticking out horizontally.
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            item.submit(poseStack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }));
    }
}
