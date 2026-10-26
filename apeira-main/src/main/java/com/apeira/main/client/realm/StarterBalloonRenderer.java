package com.apeira.main.client.realm;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.TintedBones;
import com.apeira.decor.paint.Paint;
import com.apeira.main.realm.balloon.StarterBalloonBlockEntity;
import com.apeira.main.registry.MainRealmBlocks;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Draws the realm's starter balloon: Ren's GeckoLib model ({@code geckolib/models/block/starter_balloon.geo.json}) with
 * its animated texture ({@code textures/block/starter_balloon.png}, three 256x256 frames of the burner's flame), looping
 * {@code RealmBalloonFloating}. The flame's texels have alpha 254, so the emissive render type draws them at full
 * brightness ({@link EmissiveRenderTypes}); the realm settings' paint tints every bone whose
 * name ends with {@code _tint} (the envelope, {@code balloon_tint}), and only those: their child bones and the rest
 * keep the texture's colours ({@link TintedBones#submitSuffixed}). The block's {@code facing} turns the model, whose
 * front is north (GeckoLib's own block rotation).
 */
public class StarterBalloonRenderer extends GeoBlockRenderer<StarterBalloonBlockEntity, BlockEntityRenderState> {
    /** The paint's tint, or -1 for none. */
    /** Bones whose name ends with this take the paint; the rest keep the texture's colours. */
    private static final String TINT = "_tint";
    private static final DataTicket<Integer> PAINT = DataTicket.create("apeira_balloon_paint", Integer.class);

    public StarterBalloonRenderer(BlockEntityRendererProvider.Context context) {
        super(context, MainRealmBlocks.STARTER_BALLOON_ENTITY.get());
    }

    @Override
    public void addRenderData(StarterBalloonBlockEntity balloon, @Nullable Void relatedObject, BlockEntityRenderState renderState,
            float partialTick) {
        Integer paint = balloon.paint();
        renderState.addGeckolibData(PAINT, paint != null ? Paint.tint(paint) : -1);
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return EmissiveRenderTypes.entityTranslucent(texture);
    }

    @Override
    public void submitRenderTasks(RenderPassInfo<BlockEntityRenderState> renderPassInfo, OrderedSubmitNodeCollector renderTasks,
            @Nullable RenderType renderType) {
        if (!TintedBones.submitSuffixed(renderPassInfo, renderTasks, renderType, renderPassInfo.getOrDefaultGeckolibData(PAINT, -1), TINT)) {
            super.submitRenderTasks(renderPassInfo, renderTasks, renderType);
        }
    }

    /** The envelope spans 66 pixels across the 3x3 basket and reaches 134 pixels up. */
    @Override
    public AABB getRenderBoundingBox(StarterBalloonBlockEntity balloon) {
        return new AABB(balloon.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 8.0, 0.0);
    }
}
