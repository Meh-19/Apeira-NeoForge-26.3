package com.apeira.main.client.crate;

import java.util.function.Function;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * The selected Badger Crate reward's outline: the item drawn again a little larger and a little further from the
 * camera in plain white (vanilla's outline shader: the vertex colour wherever the texture is not clear), depth tested
 * but not written, so only the rim shows round the real item and nothing in front of it is drawn over. Unlike vanilla's
 * glowing outline it is never seen through other blocks or the item itself.
 *
 * <p>An item submitted with {@link #MARK} as its outline colour goes this way only ({@code mixin/client/BadgerOutline*}).
 */
public final class SelectionOutline {
    /** An outline colour no real glowing entity uses: alpha 1. */
    public static final int MARK = 0x01FEFEFE;
    public static final int WHITE = 0xFFFFFFFF;

    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
            .withLocation(ApeiraIds.id("pipeline/badger_selection_outline"))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    private static final Function<Identifier, RenderType> TYPES = Util.memoize(texture -> RenderType.create(
            "apeira_badger_selection_outline", RenderSetup.builder(PIPELINE).withTexture("Sampler0", texture).createRenderSetup()));

    private SelectionOutline() {}

    public static RenderType type(Identifier atlas) {
        return TYPES.apply(atlas);
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }
}
