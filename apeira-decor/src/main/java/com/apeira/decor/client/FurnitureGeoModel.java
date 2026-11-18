package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.furniture.AnimatedFurnitureBlock;
import com.apeira.decor.furniture.AnimatedFurnitureBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Finds an animated furniture piece's GeckoLib files from its block id {@code <namespace>:<name>} and its block's
 * {@link AnimatedFurnitureBlock#folder() folder}: the model {@code geckolib/models/<folder>/<name>.geo.json}, the
 * animations {@code geckolib/animations/<folder>/<name>.animation.json} and the texture
 * {@code textures/block/<folder>/<name>.png}.
 */
final class FurnitureGeoModel extends GeoModel<AnimatedFurnitureBlockEntity> {
    /** The piece's files, as {@code <namespace>:<folder>/<name>}. */
    static final DataTicket<Identifier> PIECE = DataTicket.create("apeira_furniture_piece", Identifier.class);
    private static final Identifier MISSING = Identifier.withDefaultNamespace("missing");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return piece(renderState);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return piece(renderState).withPath(path -> "textures/block/" + path + ".png");
    }

    @Override
    public Identifier getAnimationResource(AnimatedFurnitureBlockEntity furniture) {
        return id(furniture);
    }

    @Override
    public void addAdditionalStateData(AnimatedFurnitureBlockEntity furniture, @Nullable Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(PIECE, id(furniture));
    }

    private static Identifier id(AnimatedFurnitureBlockEntity furniture) {
        Identifier block = BuiltInRegistries.BLOCK.getKey(furniture.getBlockState().getBlock());
        String folder = furniture.getBlockState().getBlock() instanceof AnimatedFurnitureBlock animated ? animated.folder() : "furniture";
        return block.withPrefix(folder + "/");
    }

    private static Identifier piece(GeoRenderState renderState) {
        Identifier piece = renderState.getGeckolibData(PIECE);
        return piece == null ? MISSING : piece;
    }
}
