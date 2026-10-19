package com.apeira.main.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.world.MainBiomePlacement;

import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

/** Places Xenon's biomes in a multi-noise biome source that lists them ({@link MainBiomePlacement}). */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Unique
    private MainBiomePlacement.@Nullable Placement apeira$placement;

    @Inject(method = "createResolver", at = @At("RETURN"), cancellable = true)
    private void apeira$placeXenonBiomes(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> cir) {
        cir.setReturnValue(apeira$placement().wrap(cir.getReturnValue(), sampler));
    }

    @Inject(method = "createResolverForChunk", at = @At("RETURN"), cancellable = true)
    private void apeira$placeXenonBiomesInChunk(Climate.Sampler sampler, int minQuartX, int minQuartY, int minQuartZ, int quartSizeX,
            int quartSizeY, int quartSizeZ, CallbackInfoReturnable<BiomeResolver> cir) {
        cir.setReturnValue(apeira$placement().wrap(cir.getReturnValue(), sampler));
    }

    @Unique
    private MainBiomePlacement.Placement apeira$placement() {
        MainBiomePlacement.Placement placement = apeira$placement;
        if (placement == null) {
            placement = MainBiomePlacement.Placement.of(((MultiNoiseBiomeSource) (Object) this).possibleBiomes());
            apeira$placement = placement;
        }
        return placement;
    }
}
