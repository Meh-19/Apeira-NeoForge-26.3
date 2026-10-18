package com.apeira.decor.client.crate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.crate.CrateBlockEntity;
import com.apeira.decor.crate.CrateState;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a crate's rolled items on the model's {@code item1}, {@code item2} and {@code item3} bones while it is open,
 * and, once they are out, each item's name above it, facing the camera. Ported from CosmoLib's {@code CrateItemLayer}.
 */
final class CrateItemLayer extends GeoRenderLayer<CrateBlockEntity, Void, BlockEntityRenderState> {
    private static final String[] BONES = {"item1", "item2", "item3"};
    /** Where each name is drawn, in blocks, in the model's turned space: above the left, middle and right item. */
    private static final float[][] LABEL_OFFSETS = {{-0.5F, 1.15F, -0.1875F}, {0.0F, 1.25F, 0.0625F}, {0.5F, 1.15F, -0.1875F}};
    private static final float ITEM_SCALE = 0.7F;
    private static final float LABEL_SCALE = 0.015F;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    /** CosmoLib's label background: black at 100/255 alpha. */
    private static final int LABEL_BACKGROUND = 0x64000000;

    private static final DataTicket<List<ItemStackRenderState>> ITEMS = DataTicket.create("apeira_crate_items",
            new TypeToken<List<ItemStackRenderState>>() {});
    private static final DataTicket<List<Component>> LABELS = DataTicket.create("apeira_crate_labels",
            new TypeToken<List<Component>>() {});

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    CrateItemLayer(GeoRenderer<CrateBlockEntity, Void, BlockEntityRenderState> renderer, ItemModelResolver itemModelResolver, Font font) {
        super(renderer);
        this.itemModelResolver = itemModelResolver;
        this.font = font;
    }

    @Override
    public void addRenderData(CrateBlockEntity crate, @Nullable Void relatedObject, BlockEntityRenderState renderState, float partialTick) {
        CrateState state = crate.state();
        // Render states are reused between frames, so the lists are always set, empty when there is nothing to draw.
        if (!state.inSession()) {
            renderState.addGeckolibData(ITEMS, List.of());
            renderState.addGeckolibData(LABELS, List.of());
            return;
        }
        List<ItemStackRenderState> items = new ArrayList<>(BONES.length);
        List<Component> labels = new ArrayList<>(BONES.length);
        List<ItemStack> stacks = crate.items();
        for (int i = 0; i < BONES.length; i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            ItemStackRenderState item = new ItemStackRenderState();
            if (!stack.isEmpty()) {
                itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.GROUND, crate.getLevel(), null,
                        (int) crate.getBlockPos().asLong() + i);
            }
            items.add(item);
            labels.add(stack.isEmpty() ? Component.empty() : stack.getHoverName());
        }
        renderState.addGeckolibData(ITEMS, items);
        renderState.addGeckolibData(LABELS, state == CrateState.LOOP ? labels : List.of());
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<BlockEntityRenderState> renderPassInfo,
            BiConsumer<GeoBone, PerBoneRender<BlockEntityRenderState>> consumer) {
        List<ItemStackRenderState> items = renderPassInfo.getOrDefaultGeckolibData(ITEMS, List.of());
        if (!renderPassInfo.willRender()) {
            return;
        }
        for (int i = 0; i < BONES.length && i < items.size(); i++) {
            ItemStackRenderState item = items.get(i);
            if (item.isEmpty()) {
                continue;
            }
            renderPassInfo.model().getBone(BONES[i]).ifPresent(bone -> consumer.accept(bone, (info, renderBone, tasks) -> {
                PoseStack poseStack = info.poseStack();
                poseStack.pushPose();
                poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                item.submit(poseStack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }));
        }
    }

    @Override
    public void submitRenderTask(RenderPassInfo<BlockEntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        List<Component> labels = renderPassInfo.getOrDefaultGeckolibData(LABELS, List.of());
        PoseStack poseStack = renderPassInfo.poseStack();
        float yaw = CrateRenderer.yaw(renderPassInfo.renderState());
        for (int i = 0; i < labels.size() && i < LABEL_OFFSETS.length; i++) {
            Component label = labels.get(i);
            if (label.getString().isEmpty()) {
                continue;
            }
            float[] offset = LABEL_OFFSETS[i];
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            // Undo the crate's turn, then face the camera like a name tag.
            poseStack.rotateDegrees(Axis.YP, yaw - 180.0F);
            poseStack.rotate(renderPassInfo.cameraState().orientation);
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            FormattedCharSequence text = label.getVisualOrderText();
            renderTasks.submitText(poseStack, -font.width(text) / 2.0F, 0.0F, text, false, Font.DisplayMode.NORMAL,
                    renderPassInfo.packedLight(), LABEL_COLOR, LABEL_BACKGROUND, 0);
            poseStack.popPose();
        }
    }
}
