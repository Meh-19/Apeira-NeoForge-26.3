package com.apeira.decor.client.crate;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.crate.CrateBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;

/**
 * Finds a crate's GeckoLib files from its block id {@code <namespace>:<name>}: the model
 * {@code assets/<namespace>/geckolib/models/crate/<name>.geo.json}, the animations
 * {@code geckolib/animations/crate/<name>.animation.json} and the texture {@code textures/entity/crate/<name>.png}.
 */
final class CrateModel extends GeoModel<CrateBlockEntity> {
    static final DataTicket<Identifier> CRATE = DataTicket.create("apeira_crate", Identifier.class);
    private static final Identifier MISSING = Identifier.withDefaultNamespace("missing");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return crate(renderState).withPrefix("crate/");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return crate(renderState).withPath(path -> "textures/entity/crate/" + path + ".png");
    }

    @Override
    public Identifier getAnimationResource(CrateBlockEntity crate) {
        return crate.crateId().withPrefix("crate/");
    }

    @Override
    public void addAdditionalStateData(CrateBlockEntity crate, @Nullable Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(CRATE, crate.crateId());
    }

    private static Identifier crate(GeoRenderState renderState) {
        Identifier crate = renderState.getGeckolibData(CRATE);
        return crate == null ? MISSING : crate;
    }
}
