package com.apeira.main.client.crate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.main.crate.BadgerCrateShowEntity;
import com.apeira.main.crate.BadgerTier;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Badger Crate opening ({@link BadgerCrateShowEntity}): Ren's model and animation as exported by
 * {@code tools/badger-crate-export}, with {@link BadgerCrateFx}'s texture (each reward's VFX in its tier's colour), the
 * rolled rewards on {@code item_1}-{@code item_3} and their names on {@code item_<n>_text}. While the rewards can be
 * picked, the selected one has a white outline and "Selected" under its name.
 */
public class BadgerCrateRenderer extends GeoEntityRenderer<BadgerCrateShowEntity, EntityRenderState> {
    private static final DataTicket<Integer> AGE = DataTicket.create("apeira_badger_age", Integer.class);

    public BadgerCrateRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.0F;
        withRenderLayer(new Rewards(this, context.getItemModelResolver(), context.getFont()));
    }

    /** The model reaches blocks past the entity's tiny box, and only its opener ever sees it. */
    @Override
    public boolean shouldRender(BadgerCrateShowEntity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return true;
    }

    @Override
    public void addRenderData(BadgerCrateShowEntity entity, @Nullable Void relatedObject, EntityRenderState renderState, float partialTick) {
        BadgerCrateFx.update(new BadgerTier[] {entity.tier(0), entity.tier(1), entity.tier(2)}, entity.tickCount);
        renderState.addGeckolibData(AGE, entity.tickCount);
    }

    /** Translucent (the VFX fade), with alpha 254 texels glowing ({@link EmissiveRenderTypes}). */
    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return EmissiveRenderTypes.entityTranslucent(texture);
    }

    /** {@code geckolib/models/badger_crate.geo.json} and {@code geckolib/animations/badger_crate.animation.json}. */
    private static final class Model extends GeoModel<BadgerCrateShowEntity> {
        private static final Identifier ID = ApeiraIds.id("badger_crate");

        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
            return ID;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            return BadgerCrateFx.ready() ? BadgerCrateFx.TEXTURE : BadgerCrateFx.BASE;
        }

        @Override
        public Identifier getAnimationResource(BadgerCrateShowEntity animatable) {
            return ID;
        }
    }

    /** The rolled rewards, their names and the selection. */
    private static final class Rewards extends GeoRenderLayer<BadgerCrateShowEntity, Void, EntityRenderState> {
        private static final String[] ITEM_BONES = {"item_1", "item_2", "item_3"};
        private static final String[] TEXT_BONES = {"item_1_text", "item_2_text", "item_3_text"};
        private static final float ITEM_SCALE = 1.5F;
        private static final float LABEL_SCALE = 0.025F;
        private static final int OUTLINE = 0xFFFFFFFF;
        private static final int LABEL_BACKGROUND = 0x40000000;
        private static final int[] TIER_COLOURS = {0xFFFFFFFF, 0xFF55AAFF, 0xFFBB66FF, 0xFFFFCC33};

        private static final DataTicket<List<ItemStackRenderState>> ITEMS = DataTicket.create("apeira_badger_items",
                new TypeToken<List<ItemStackRenderState>>() {});
        private static final DataTicket<List<Component>> LABELS = DataTicket.create("apeira_badger_labels",
                new TypeToken<List<Component>>() {});
        private static final DataTicket<Integer> SELECTED = DataTicket.create("apeira_badger_selected", Integer.class);

        private final ItemModelResolver itemModelResolver;
        private final Font font;

        Rewards(GeoRenderer<BadgerCrateShowEntity, Void, EntityRenderState> renderer, ItemModelResolver itemModelResolver, Font font) {
            super(renderer);
            this.itemModelResolver = itemModelResolver;
            this.font = font;
        }

        @Override
        public void addRenderData(BadgerCrateShowEntity entity, @Nullable Void relatedObject, EntityRenderState renderState, float partialTick) {
            List<ItemStackRenderState> items = new ArrayList<>(3);
            List<Component> labels = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                ItemStack stack = entity.item(i);
                ItemStackRenderState item = new ItemStackRenderState();
                if (!stack.isEmpty()) {
                    itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.GROUND, entity.level(), null, entity.getId() * 3 + i);
                }
                items.add(item);
                labels.add(stack.isEmpty() ? Component.empty() : stack.getHoverName().copy().withColor(TIER_COLOURS[entity.tier(i).ordinal()]));
            }
            renderState.addGeckolibData(ITEMS, items);
            renderState.addGeckolibData(LABELS, labels);
            renderState.addGeckolibData(SELECTED, entity.selecting() ? BadgerCrateClient.selected(entity) : -1);
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<EntityRenderState> renderPassInfo,
                BiConsumer<GeoBone, PerBoneRender<EntityRenderState>> consumer) {
            if (!renderPassInfo.willRender()) {
                return;
            }
            List<ItemStackRenderState> items = renderPassInfo.getOrDefaultGeckolibData(ITEMS, List.of());
            List<Component> labels = renderPassInfo.getOrDefaultGeckolibData(LABELS, List.of());
            int selected = renderPassInfo.getOrDefaultGeckolibData(SELECTED, -1);
            for (int i = 0; i < 3 && i < items.size(); i++) {
                ItemStackRenderState item = items.get(i);
                int slot = i;
                if (!item.isEmpty()) {
                    renderPassInfo.model().getBone(ITEM_BONES[i]).ifPresent(bone -> consumer.accept(bone, (info, renderBone, tasks) -> {
                        PoseStack poseStack = info.poseStack();
                        poseStack.pushPose();
                        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                        item.submit(poseStack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, slot == selected ? OUTLINE : 0);
                        poseStack.popPose();
                    }));
                }
                if (i < labels.size() && !labels.get(i).getString().isEmpty()) {
                    Component label = labels.get(i);
                    renderPassInfo.model().getBone(TEXT_BONES[i]).ifPresent(bone -> consumer.accept(bone,
                            (info, renderBone, tasks) -> label(info, tasks, label, slot == selected)));
                }
            }
        }

        /** The name facing the camera at the text bone, hidden while the bone is scaled away, "Selected" under it. */
        private void label(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector tasks, Component label, boolean selected) {
            PoseStack poseStack = info.poseStack();
            Matrix4f pose = poseStack.last().pose();
            Vector3f scale = pose.getScale(new Vector3f());
            if (scale.x() < 0.01F || scale.y() < 0.01F) {
                return;
            }
            Vector3f position = pose.getTranslation(new Vector3f());
            poseStack.pushPose();
            poseStack.last().pose().identity().translate(position).rotate(info.cameraState().orientation)
                    .scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            poseStack.last().normal().identity();
            text(poseStack, tasks, label.getVisualOrderText(), 0, info.packedLight());
            if (selected) {
                text(poseStack, tasks, Component.translatable("message.apeira.badger_crate.selected").getVisualOrderText(),
                        font.lineHeight + 2, info.packedLight());
            }
            poseStack.popPose();
        }

        private void text(PoseStack poseStack, SubmitNodeCollector tasks, FormattedCharSequence text, int y, int light) {
            tasks.submitText(poseStack, -font.width(text) / 2.0F, y, text, false, Font.DisplayMode.NORMAL,
                    light, 0xFFFFFFFF, LABEL_BACKGROUND, 0);
        }
    }
}
