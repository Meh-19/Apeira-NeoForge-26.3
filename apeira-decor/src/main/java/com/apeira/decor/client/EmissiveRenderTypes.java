package com.apeira.decor.client;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Apeira's alpha 254 rule: a texel whose alpha is exactly 254 ({@code 0xFE}) is emissive, drawn at full brightness and
 * full opacity whatever the light around it; alpha 255 is opaque and anything lower is blended as usual. Texture
 * painters mark flames, lamps and glowing runes this way (the starter balloon's burner is the first), with no glow mask
 * texture beside the main one.
 *
 * <p>The rule lives in the {@code apeira:core/emissive_entity} and {@code apeira:core/emissive_item} shaders, vanilla's
 * entity and item shaders plus {@code apeira:emissive.glsl}, and in the finish shaders ({@link FinishRenderTypes}).
 * GeckoLib renderers (the starter balloon, the ruby deposit, animated artifacts, crates) draw with
 * {@link #entityTranslucent}; every item quad drawn with vanilla's translucent item sheets, which is where any sprite
 * with a 254 texel ends up, is moved to the emissive item types by {@code mixin/ItemFeatureRendererMixin}
 * ({@link #forItem}), so placed furniture, artifacts, cosmetics and held items follow the rule too.
 */
public final class EmissiveRenderTypes {
    /** The alpha of an emissive texel. */
    public static final int EMISSIVE_ALPHA = 0xFE;

    private static final RenderPipeline.Snippet ENTITY_SHADER = RenderPipeline.builder()
            .withVertexShader(ApeiraIds.id("core/emissive_entity"))
            .withFragmentShader(ApeiraIds.id("core/emissive_entity"))
            .buildSnippet();
    private static final RenderPipeline.Snippet ITEM_SHADER = RenderPipeline.builder()
            .withVertexShader(ApeiraIds.id("core/emissive_item"))
            .withFragmentShader(ApeiraIds.id("core/emissive_item"))
            .buildSnippet();

    // Vanilla's RenderPipelines.ENTITY_TRANSLUCENT and OIT_ENTITY with the emissive shaders, with and without culling.
    public static final RenderPipeline ENTITY_PIPELINE = entityPipeline("pipeline/emissive_entity_translucent", false);
    public static final RenderPipeline ENTITY_CULL_PIPELINE = entityPipeline("pipeline/emissive_entity_translucent_cull", true);
    public static final OitPipelineSet OIT_ENTITY = oitEntity("emissive_entity", false);
    public static final OitPipelineSet OIT_ENTITY_CULL = oitEntity("emissive_entity_cull", true);

    // Vanilla's RenderPipelines.ITEM_TRANSLUCENT and OIT_ITEM with the emissive shaders.
    public static final RenderPipeline ITEM_PIPELINE = RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET, ITEM_SHADER)
            .withLocation(ApeiraIds.id("pipeline/emissive_item_translucent"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    public static final OitPipelineSet OIT_ITEM = OitPipelineSet.builder(ApeiraIds.id("emissive_item"),
                    RenderPipeline.builder(RenderPipelines.OIT_ITEM_SNIPPET, ITEM_SHADER))
            .withAccumulateModifier(accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER2))
            .build();

    // Vanilla's RenderTypes.entityTranslucent and entityTranslucentCull with the emissive pipelines.
    private static final BiFunction<Identifier, Boolean, RenderType> ENTITY = Util.memoize((texture, cull) -> RenderType.create(
            cull ? "apeira_emissive_entity_translucent_cull" : "apeira_emissive_entity_translucent",
            RenderSetup.builder(cull ? ENTITY_CULL_PIPELINE : ENTITY_PIPELINE)
                    .setOitPipelines(cull ? OIT_ENTITY_CULL : OIT_ENTITY)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    // Vanilla's RenderTypes.itemTranslucent with the emissive pipeline.
    private static final Function<Identifier, RenderType> ITEM = Util.memoize(texture -> RenderType.create(
            "apeira_emissive_item_translucent", RenderSetup.builder(ITEM_PIPELINE)
                    .setOitPipelines(OIT_ITEM)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));

    private static @Nullable Map<RenderType, RenderType> itemTypes;

    private EmissiveRenderTypes() {}

    private static RenderPipeline entityPipeline(String location, boolean cull) {
        return RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET, ENTITY_SHADER)
                .withLocation(ApeiraIds.id(location))
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withShaderDefine("PER_FACE_LIGHTING")
                .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(cull)
                .build();
    }

    private static OitPipelineSet oitEntity(String name, boolean cull) {
        return OitPipelineSet.builder(ApeiraIds.id(name), RenderPipeline.builder(RenderPipelines.OIT_ENTITY_SNIPPET, ENTITY_SHADER).withCull(cull))
                .withAccumulateModifier(accumulate -> accumulate.withShaderDefine("PER_FACE_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER2))
                .build();
    }

    /** Registers the emissive pipelines; on the mod event bus. */
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ENTITY_PIPELINE);
        event.registerPipeline(ENTITY_CULL_PIPELINE);
        event.registerPipeline(ITEM_PIPELINE);
        event.registerOitPipelineSet(OIT_ENTITY);
        event.registerOitPipelineSet(OIT_ENTITY_CULL);
        event.registerOitPipelineSet(OIT_ITEM);
    }

    /** {@code RenderTypes.entityTranslucent} (no culling) following the alpha 254 rule, for GeckoLib models. */
    public static RenderType entityTranslucent(Identifier texture) {
        return ENTITY.apply(texture, false);
    }

    /** {@code RenderTypes.entityTranslucentCull} following the alpha 254 rule. */
    public static RenderType entityTranslucentCull(Identifier texture) {
        return ENTITY.apply(texture, true);
    }

    /** The emissive version of a translucent item sheet, else {@code renderType} itself. */
    public static RenderType forItem(RenderType renderType) {
        if (itemTypes == null) {
            itemTypes = Map.of(
                    Sheets.translucentBlockItemSheet(), ITEM.apply(TextureAtlas.LOCATION_BLOCKS),
                    Sheets.translucentItemSheet(), ITEM.apply(TextureAtlas.LOCATION_ITEMS));
        }
        return itemTypes.getOrDefault(renderType, renderType);
    }
}
