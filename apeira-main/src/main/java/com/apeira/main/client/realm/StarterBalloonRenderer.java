package com.apeira.main.client.realm;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.PlacementGlide;
import com.apeira.decor.client.TintedBones;
import com.apeira.decor.paint.Paint;
import com.apeira.main.realm.balloon.StarterBalloonBlockEntity;
import com.apeira.main.registry.MainRealmBlocks;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the realm's starter balloon: Ren's GeckoLib model ({@code geckolib/models/block/starter_balloon.geo.json}) with
 * its animated texture ({@code textures/block/starter_balloon.png}, three 256x256 frames of the burner's flame), looping
 * {@code RealmBalloonFloating}. The flame's texels have alpha 254, so the emissive render type draws them at full
 * brightness ({@link EmissiveRenderTypes}); the realm settings' paint tints every bone whose
 * name ends with {@code _tint} (the envelope, {@code balloon_tint}), and only those: their child bones and the rest
 * keep the texture's colours ({@link TintedBones#submitSuffixed}). The block's {@code facing} turns the model, whose
 * front is north (GeckoLib's own block rotation). A balloon the spawn placement just moved glides from its old spot to
 * its new one ({@link #glide}, {@link PlacementGlide}) instead of appearing there at once.
 */
public class StarterBalloonRenderer extends GeoBlockRenderer<StarterBalloonBlockEntity, BlockEntityRenderState> {
    /** The paint's tint, or -1 for none. */
    /** Bones whose name ends with this take the paint; the rest keep the texture's colours. */
    private static final String TINT = "_tint";
    private static final DataTicket<Integer> PAINT = DataTicket.create("apeira_balloon_paint", Integer.class);
    private static final DataTicket<Vec3> GLIDE = DataTicket.create("apeira_balloon_glide", Vec3.class);

    /** The client's balloon's glide to its new spot, while one runs. */
    private static @Nullable PlacementGlide glide;

    /** Starts the balloon's glide from {@code from} to {@code to}, where the server has just moved it. */
    public static void glide(BlockPos from, BlockPos to) {
        glide = PlacementGlide.between(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to), 0.0F);
    }

    /** The running glide of the balloon at {@code pos}, if any. */
    private static @Nullable PlacementGlide glideAt(BlockPos pos) {
        PlacementGlide current = glide;
        if (current == null || current.done()) {
            glide = null;
            return null;
        }
        return current.to().equals(Vec3.atLowerCornerOf(pos)) ? current : null;
    }

    public StarterBalloonRenderer(BlockEntityRendererProvider.Context context) {
        super(context, MainRealmBlocks.STARTER_BALLOON_ENTITY.get());
    }

    @Override
    public void addRenderData(StarterBalloonBlockEntity balloon, @Nullable Void relatedObject, BlockEntityRenderState renderState,
            float partialTick) {
        Integer paint = balloon.paint();
        renderState.addGeckolibData(PAINT, paint != null ? Paint.tint(paint) : -1);
        PlacementGlide current = glideAt(balloon.getBlockPos());
        renderState.addGeckolibData(GLIDE, current != null ? current.offset() : Vec3.ZERO);
    }

    /** Draws a gliding balloon at its glide's offset from its block. */
    @Override
    public void preRenderPass(RenderPassInfo<BlockEntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        super.preRenderPass(renderPassInfo, renderTasks);
        Vec3 offset = renderPassInfo.getOrDefaultGeckolibData(GLIDE, Vec3.ZERO);
        renderPassInfo.poseStack().translate(offset.x, offset.y, offset.z);
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
        AABB box = new AABB(balloon.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 8.0, 0.0);
        PlacementGlide current = glideAt(balloon.getBlockPos());
        // While gliding it is drawn between its old and new spots.
        return current != null ? box.minmax(box.move(current.from().subtract(current.to()))).expandTowards(0.0, 1.0, 0.0) : box;
    }
}
