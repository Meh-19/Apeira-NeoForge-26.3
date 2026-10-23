package com.apeira.main.client.ruby;

import com.apeira.main.registry.MainRubies;
import com.apeira.main.ruby.RubyDepositBlockEntity;
import com.geckolib.renderer.GeoBlockRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the ruby deposit's GeckoLib model ({@code geckolib/models/block/ruby_deposit.geo.json}, Ren's Blockbench
 * model) with its animated texture ({@code textures/block/ruby_deposit.png}, eight 64x64 frames, which GeckoLib plays
 * from its {@code .mcmeta}) and loops its {@code ruby_deposit} animation: the crystal turns and bobs over the base.
 */
public class RubyDepositRenderer extends GeoBlockRenderer<RubyDepositBlockEntity, BlockEntityRenderState> {
    public RubyDepositRenderer(BlockEntityRendererProvider.Context context) {
        super(context, MainRubies.RUBY_DEPOSIT_ENTITY.get());
    }

    /** The crystal floats up to 35 pixels over the block's floor, so the model is culled by a taller box. */
    @Override
    public AABB getRenderBoundingBox(RubyDepositBlockEntity deposit) {
        return new AABB(deposit.getBlockPos()).expandTowards(0.0, 1.5, 0.0);
    }
}
