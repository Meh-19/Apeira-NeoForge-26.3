package com.apeira.decor.client;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.apeira.decor.mixin.PictureInPictureRendererAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Draws an entity in a GUI as {@link GuiGraphicsExtractor#entity} does, with a crisp black toon outline one GUI pixel
 * wide around its silhouette: the entity is drawn into its picture-in-picture texture once, and that texture is blitted
 * eight times in black, shifted one GUI pixel each way, under the normal blit. Registered for
 * {@link OutlinedEntityRenderState} in {@code ApeiraDecorClient}.
 */
public final class OutlinedEntityRenderer extends PictureInPictureRenderer<OutlinedEntityRenderer.OutlinedEntityRenderState> {
    /** The outline's colour: vertex colour multiplies the texture, so black keeps only its alpha. */
    private static final int OUTLINE = 0xFF000000;
    private static final int[][] OFFSETS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    /**
     * An entity to draw outlined, as {@link net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState}; its bounds
     * reach one pixel past the box for the outline.
     */
    public record OutlinedEntityRenderState(EntityRenderState renderState, Vector3fc translation, Quaternionfc rotation, int x0, int y0, int x1,
            int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {}

    /**
     * Draws {@code state} in the box {@code x0, y0, x1, y1} with an outline, taking the same arguments as
     * {@link GuiGraphicsExtractor#entity} without a camera override.
     */
    public static void submit(GuiGraphicsExtractor graphics, EntityRenderState state, float scale, Vector3fc translation, Quaternionfc rotation,
            int x0, int y0, int x1, int y1) {
        state.lightCoords = 15728880;
        graphics.submitPictureInPictureRenderState(new OutlinedEntityRenderState(state, translation, rotation, x0, y0, x1, y1, scale, null,
                PictureInPictureRenderState.getBounds(x0 - 1, y0 - 1, x1 + 1, y1 + 1, null)));
    }

    @Override
    public Class<OutlinedEntityRenderState> getRenderStateClass() {
        return OutlinedEntityRenderState.class;
    }

    @Override
    protected void renderToTexture(OutlinedEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        poseStack.translate(state.translation().x(), state.translation().y(), state.translation().z());
        poseStack.rotate(state.rotation());
        minecraft.getEntityRenderDispatcher().submit(state.renderState(), new CameraRenderState(), 0.0, 0.0, 0.0, poseStack, collector);
    }

    @Override
    protected void blitTexture(OutlinedEntityRenderState state, GuiRenderState guiRenderState) {
        GpuTextureView texture = ((PictureInPictureRendererAccessor) (Object) this).apeira$textureView();
        if (texture != null) {
            TextureSetup setup = TextureSetup.singleTexture(texture, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
            for (int[] offset : OFFSETS) {
                guiRenderState.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, setup, state.pose(),
                        state.x0() + offset[0], state.y0() + offset[1], state.x1() + offset[0], state.y1() + offset[1], 0.0F, 1.0F, 1.0F, 0.0F,
                        OUTLINE, state.scissorArea(), null));
            }
        }
        super.blitTexture(state, guiRenderState);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "outlined entity";
    }
}
