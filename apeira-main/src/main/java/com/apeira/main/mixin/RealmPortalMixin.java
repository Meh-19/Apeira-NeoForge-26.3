package com.apeira.main.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.realm.RealmLevels;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

/**
 * Lets fire light nether portals in realms: vanilla lights them only in {@code minecraft:overworld} and
 * {@code minecraft:the_nether} ({@code BaseFireBlock#inPortalDimension}), so in a realm flint and steel only set the
 * obsidian alight. Where the portals lead is {@link RealmPortalDestinationMixin}.
 */
@Mixin(BaseFireBlock.class)
public abstract class RealmPortalMixin {
    @Inject(method = "inPortalDimension", at = @At("HEAD"), cancellable = true)
    private static void apeira$lightPortalsInRealms(Level level, CallbackInfoReturnable<Boolean> cir) {
        if (RealmLevels.isRealm(level.dimension())) {
            cir.setReturnValue(true);
        }
    }
}
