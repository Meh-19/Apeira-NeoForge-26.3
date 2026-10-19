package com.apeira.archae.client;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.block.AnimatedArtifactBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * The GeckoLib model of an animated artifact, found by its block id as Argon's {@code IdBasedFurnitureModel} found it:
 * {@code geckolib/models/archaeology/<id>.geo.json}, {@code geckolib/animations/archaeology/<id>.animation.json} and
 * {@code textures/block/archaeology/<id>.png}.
 */
final class ArtifactGeoModel extends GeoModel<AnimatedArtifactBlockEntity> {
    static final DataTicket<Identifier> ARTIFACT = DataTicket.create("apeira_artifact", Identifier.class);
    private static final Identifier MISSING = Identifier.withDefaultNamespace("missing");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return artifact(renderState).withPrefix("archaeology/");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return artifact(renderState).withPath(path -> "textures/block/archaeology/" + path + ".png");
    }

    @Override
    public Identifier getAnimationResource(AnimatedArtifactBlockEntity artifact) {
        return id(artifact).withPrefix("archaeology/");
    }

    @Override
    public void addAdditionalStateData(AnimatedArtifactBlockEntity artifact, @Nullable Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(ARTIFACT, id(artifact));
    }

    private static Identifier id(AnimatedArtifactBlockEntity artifact) {
        return BuiltInRegistries.BLOCK.getKey(artifact.getBlockState().getBlock());
    }

    private static Identifier artifact(GeoRenderState renderState) {
        Identifier artifact = renderState.getGeckolibData(ARTIFACT);
        return artifact == null ? MISSING : artifact;
    }
}
