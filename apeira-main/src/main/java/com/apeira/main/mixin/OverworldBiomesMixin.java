package com.apeira.main.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.world.MainBiomePlacement;
import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

/**
 * Lists Xenon's biomes in the overworld preset, at climate points no world reaches ({@link MainBiomePlacement}). Not
 * during datagen: vanilla's own registry set, built there, has no Apeira biomes to resolve the keys against.
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomesMixin {
    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void apeira$addXenonBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, CallbackInfo ci) {
        if (!DatagenModLoader.isRunningDataGen()) {
            MainBiomePlacement.addUnreachablePoints(biomes);
        }
    }
}
