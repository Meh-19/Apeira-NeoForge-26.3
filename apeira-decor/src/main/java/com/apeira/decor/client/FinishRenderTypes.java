package com.apeira.decor.client;

import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.Paint;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Draws paint finishes on items, CosmoLib's animated finish shaders ported to render pipelines.
 *
 * <p>An item's {@code apeira:paint} tint ({@link PaintTintSource}) is the finish's paint value with alpha 254
 * ({@link Paint#itemTint}). When the item renderer meets a quad with that tint ({@code mixin/ItemFeatureRendererMixin}),
 * it draws the quad with the matching finish render type below instead of vanilla's item render type, at full alpha.
 * The finish render types use the {@code apeira:core/finish_item} shaders, vanilla's item shaders plus CosmoLib's
 * finish code ({@code shaders/include/finish/}), which read the finish from the vertex colour as CosmoLib's did and
 * sample the finish atlas ({@link #ATLAS}) as {@code FinishSampler}.
 *
 * <p>Only the cutout and translucent item render types have finish versions. Enchanted (glint) items and anything else
 * draw the finish's representative colour.
 */
public final class FinishRenderTypes {
    /** The textures the finishes are drawn from, CosmoLib's {@code cosmolib:textures/misc/finish_atlas.png}. */
    public static final Identifier ATLAS = ApeiraIds.id("textures/misc/finish_atlas.png");
    /** The alpha of a finish tint, which tells it apart from any other item tint of the same colour. */
    public static final int TINT_ALPHA = 0xFE;

    private static final Identifier SHADER = ApeiraIds.id("core/finish_item");
    private static final BindGroupLayout FINISH_SAMPLER = BindGroupLayout.builder()
            .withUniform("FinishSampler", UniformType.COMBINED_IMAGE_SAMPLER)
            .build();
    private static final RenderPipeline.Snippet FINISH_SNIPPET = RenderPipeline.builder()
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withBindGroupLayout(FINISH_SAMPLER)
            .buildSnippet();

    // Vanilla's item pipelines (RenderPipelines.ITEM_CUTOUT, ITEM_TRANSLUCENT and OIT_ITEM) with the finish shaders.
    public static final RenderPipeline CUTOUT_PIPELINE = RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET, FINISH_SNIPPET)
            .withLocation(ApeiraIds.id("pipeline/finish_item_cutout"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build();
    public static final RenderPipeline TRANSLUCENT_PIPELINE = RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET, FINISH_SNIPPET)
            .withLocation(ApeiraIds.id("pipeline/finish_item_translucent"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    public static final OitPipelineSet OIT_PIPELINES = OitPipelineSet.builder(ApeiraIds.id("finish_item"),
                    RenderPipeline.builder(RenderPipelines.OIT_ITEM_SNIPPET, FINISH_SNIPPET))
            .withAccumulateModifier(accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER2))
            .build();

    // Vanilla's RenderTypes.itemCutout and itemTranslucent, with the finish atlas.
    private static final Function<Identifier, RenderType> CUTOUT = Util.memoize(texture -> RenderType.create(
            "apeira_finish_item_cutout", RenderSetup.builder(CUTOUT_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .withTexture("FinishSampler", ATLAS)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> TRANSLUCENT = Util.memoize(texture -> RenderType.create(
            "apeira_finish_item_translucent", RenderSetup.builder(TRANSLUCENT_PIPELINE)
                    .setOitPipelines(OIT_PIPELINES)
                    .withTexture("Sampler0", texture)
                    .withTexture("FinishSampler", ATLAS)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));

    private static @Nullable Map<RenderType, RenderType> finishTypes;

    private FinishRenderTypes() {}

    /** Registers the finish pipelines; on the mod event bus. */
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CUTOUT_PIPELINE);
        event.registerPipeline(TRANSLUCENT_PIPELINE);
        event.registerOitPipelineSet(OIT_PIPELINES);
    }

    /** Whether the stack is painted with a finish, so its look changes every frame. */
    public static boolean hasFinish(ItemStack stack) {
        Integer paint = Paint.get(stack);
        return paint != null && FinishType.fromPaint(paint) != null;
    }

    /**
     * The render type to draw an item quad with: the finish version of {@code renderType} when the quad is tinted with
     * a finish, else {@code renderType}. A finish quad is given full alpha, or its representative colour when
     * {@code renderType} has no finish version.
     */
    public static RenderType forQuad(RenderType renderType, QuadInstance quad) {
        int tint = quad.getColor(0);
        if (tint >>> 24 != TINT_ALPHA || FinishType.fromPaint(tint) == null) {
            return renderType;
        }
        RenderType finish = finishTypes().get(renderType);
        quad.setColor(finish != null ? tint | 0xFF000000 : Paint.tint(tint & 0xFFFFFF));
        return finish != null ? finish : renderType;
    }

    private static Map<RenderType, RenderType> finishTypes() {
        if (finishTypes == null) {
            finishTypes = Map.of(
                    Sheets.cutoutBlockItemSheet(), CUTOUT.apply(TextureAtlas.LOCATION_BLOCKS),
                    Sheets.cutoutItemSheet(), CUTOUT.apply(TextureAtlas.LOCATION_ITEMS),
                    Sheets.translucentBlockItemSheet(), TRANSLUCENT.apply(TextureAtlas.LOCATION_BLOCKS),
                    Sheets.translucentItemSheet(), TRANSLUCENT.apply(TextureAtlas.LOCATION_ITEMS));
        }
        return finishTypes;
    }
}
