package com.apeira.main.client.farming;

import org.jspecify.annotations.Nullable;

import com.apeira.main.farming.FarmCrop;
import com.apeira.main.farming.PlanterBoxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a planter box's crop on its top: Origin Realms' model of the crop's stage (or of the golden crop), an item
 * definition ({@code items/crop/<model>.json}, {@link FarmCrop#model}) drawn in the {@code fixed} display context at
 * full size. Origin Realms showed these models in item frames on the box, and some trees reach below their model's
 * floor; each model's {@code fixed} transform is only the lift that puts its trunk on the soil, worked out from the
 * server's own frame transform.
 */
public final class PlanterBoxRenderer implements BlockEntityRenderer<PlanterBoxBlockEntity, PlanterBoxRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public PlanterBoxRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlanterBoxBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        FarmCrop crop = blockEntity.crop();
        if (crop == null || blockEntity.stage() < 1) {
            state.item.clear();
            return;
        }
        ItemStack stack = new ItemStack(Items.WHEAT_SEEDS);
        stack.set(DataComponents.ITEM_MODEL, crop.model(Math.min(blockEntity.stage(), crop.stages()), blockEntity.isGolden()));
        itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null,
                (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Items are drawn centred on the origin: half a block in from the corner, and a block and a half up puts the
        // model's floor on the box's top.
        poseStack.translate(0.5F, 1.5F, 0.5F);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** Trees reach well above the box, so crops are kept while the box itself is off screen. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
    }
}
