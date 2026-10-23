package com.apeira.decor.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

/** The texture a picture-in-picture renderer drew into, which {@link com.apeira.decor.client.OutlinedEntityRenderer} blits again as its outline. */
@Mixin(PictureInPictureRenderer.class)
public interface PictureInPictureRendererAccessor {
    @Accessor("textureView")
    @Nullable GpuTextureView apeira$textureView();
}
