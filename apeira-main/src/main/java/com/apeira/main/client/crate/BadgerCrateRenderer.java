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
        /** How thick the outline is and how far behind the item its copies sit, in blocks. */
        private static final float OUTLINE_WIDTH = 0.03F;
        private static final float OUTLINE_PUSH = 0.05F;
        /** The eight directions on the screen the outline copies are moved in: a rim of even width all round. */
        private static final float[][] OUTLINE_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {0.7071F, 0.7071F}, {0.7071F, -0.7071F}, {-0.7071F, 0.7071F}, {-0.7071F, -0.7071F}};
        /** How far above its anchor a reward's name is drawn, in blocks. */
        private static final float NAME_RAISE = 0.3F;
        /** The recorded {@code selected_text} display's scale against the names'. */
        private static final float SELECTED_SCALE = LABEL_SCALE * 0.8F;
        /**
         * Where Ren's Blockbench file puts the {@code selected_text} anchor under each reward, from that reward's
         * {@code item_<n>_text} anchor, in blocks (Blockbench's axes are the world's here: the model is not turned).
         * Measured from the recording: the anchor's rest pivot and its position keys while each reward was selected.
         */
        private static final float[][] SELECTED_OFFSETS = {
                {-0.2856F / 16.0F, -20.8F / 16.0F, 6.3921F / 16.0F},
                {-2.6923F / 16.0F, -20.8F / 16.0F, 5.8062F / 16.0F},
                {-4.6918F / 16.0F, -20.8F / 16.0F, 4.3512F / 16.0F}};
        private static final int LABEL_BACKGROUND = 0;
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
                        if (slot == selected) {
                            outline(poseStack, tasks, item, info.packedLight(), info.cameraState().orientation);
                        }
                        poseStack.pushPose();
                        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                        item.submit(poseStack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
                        poseStack.popPose();
                    }));
                }
                if (i < labels.size() && !labels.get(i).getString().isEmpty()) {
                    Component label = labels.get(i);
                    renderPassInfo.model().getBone(TEXT_BONES[i]).ifPresent(bone -> consumer.accept(bone,
                            (info, renderBone, tasks) -> label(info, tasks, label, slot == selected ? slot : -1)));
                }
            }
        }

        /**
         * The white line round the selected reward's silhouette ({@link SelectionOutline}): the item drawn in plain white
         * eight times, each moved {@link #OUTLINE_WIDTH} blocks across the screen (along the camera's right and up) and
         * {@link #OUTLINE_PUSH} blocks behind the item, so the item covers the copies but their even rim round its
         * outer edge, as a flat outline of the item as seen. Copies are depth tested, so nothing in front of the item
         * shows it.
         */
        private static void outline(PoseStack poseStack, SubmitNodeCollector tasks, ItemStackRenderState item, int light,
                org.joml.Quaternionfc camera) {
            Vector3f away = poseStack.last().pose().getTranslation(new Vector3f());
            if (away.lengthSquared() > 1.0E-6F) {
                away.normalize(OUTLINE_PUSH);
            }
            Vector3f right = camera.transform(new Vector3f(1, 0, 0));
            Vector3f up = camera.transform(new Vector3f(0, 1, 0));
            for (float[] direction : OUTLINE_DIRECTIONS) {
                Vector3f shift = new Vector3f(right).mul(direction[0] * OUTLINE_WIDTH).add(new Vector3f(up).mul(direction[1] * OUTLINE_WIDTH)).add(away);
                poseStack.pushPose();
                poseStack.last().pose().translateLocal(shift);
                poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                item.submit(poseStack, tasks, light, OverlayTexture.NO_OVERLAY, SelectionOutline.MARK);
                poseStack.popPose();
            }
        }

        /**
         * The name facing the camera at the text bone, hidden while the bone is scaled away; for the selected reward
         * ({@code selectedSlot} 0 to 2, else -1), "Selected" where Ren's file has its {@code selected_text} anchor for
         * that reward ({@link #SELECTED_OFFSETS}).
         */
        private void label(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector tasks, Component label, int selectedSlot) {
            PoseStack poseStack = info.poseStack();
            Matrix4f pose = poseStack.last().pose();
            Vector3f scale = pose.getScale(new Vector3f());
            if (scale.x() < 0.01F || scale.y() < 0.01F) {
                return;
            }
            Vector3f position = pose.getTranslation(new Vector3f());
            poseStack.pushPose();
            poseStack.last().pose().identity().translate(position).translate(0.0F, NAME_RAISE, 0.0F)
                    .rotate(info.cameraState().orientation).scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            poseStack.last().normal().identity();
            text(poseStack, tasks, label.getVisualOrderText(), 0, info.packedLight());
            poseStack.popPose();
            if (selectedSlot >= 0) {
                float[] offset = SELECTED_OFFSETS[selectedSlot];
                poseStack.pushPose();
                poseStack.last().pose().identity().translate(position).translate(offset[0], offset[1], offset[2])
                        .rotate(info.cameraState().orientation).scale(SELECTED_SCALE, -SELECTED_SCALE, SELECTED_SCALE);
                poseStack.last().normal().identity();
                text(poseStack, tasks, Component.translatable("message.apeira.badger_crate.selected").getVisualOrderText(), 0,
                        info.packedLight());
                poseStack.popPose();
            }
        }

        private void text(PoseStack poseStack, SubmitNodeCollector tasks, FormattedCharSequence text, int y, int light) {
            tasks.submitText(poseStack, -font.width(text) / 2.0F, y, text, true, Font.DisplayMode.NORMAL,
                    light, 0xFFFFFFFF, LABEL_BACKGROUND, 0);
        }
    }
}
