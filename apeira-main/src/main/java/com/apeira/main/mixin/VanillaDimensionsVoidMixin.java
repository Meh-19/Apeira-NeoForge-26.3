package com.apeira.main.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.apeira.main.realm.VoidVanillaDimensions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Makes {@code minecraft:overworld} and {@code minecraft:the_nether} void while realm presets are on
 * ({@link VoidVanillaDimensions#stem}): the overworld's stem is read once by key in {@code createLevels}, every other
 * dimension's from the stem registry's entries.
 */
@Mixin(MinecraftServer.class)
public abstract class VanillaDimensionsVoidMixin {
    @ModifyExpressionValue(method = "createLevels",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;"))
    private Object apeira$voidOverworld(Object stem) {
        return VoidVanillaDimensions.stem((MinecraftServer) (Object) this, LevelStem.OVERWORLD, (LevelStem) stem);
    }

    @ModifyExpressionValue(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;"))
    private Object apeira$voidNether(Object stem, @Local Map.Entry<ResourceKey<LevelStem>, LevelStem> entry) {
        return VoidVanillaDimensions.stem((MinecraftServer) (Object) this, entry.getKey(), (LevelStem) stem);
    }
}
