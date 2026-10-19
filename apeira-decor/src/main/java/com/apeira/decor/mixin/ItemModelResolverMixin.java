package com.apeira.decor.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.decor.client.FinishRenderTypes;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Marks an item painted with a finish as animated, so the GUI redraws it every frame instead of keeping it in its item
 * atlas as it first drew it.
 */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @Inject(method = "appendItemLayers", at = @At("TAIL"))
    private void apeira$animateFinish(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext,
            @Nullable Level level, @Nullable ItemOwner owner, int seed, CallbackInfo ci) {
        if (FinishRenderTypes.hasFinish(item)) {
            output.setAnimated();
        }
    }
}
