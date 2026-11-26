package com.apeira.main.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apeira.main.realm.RealmLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * A nether portal in a realm leads to that realm's own nether and back ({@link RealmLevels#portalTarget}), never to the
 * vanilla dimensions. The realm nether's coordinate scale is 1, so an exit is looked for (and built) at the same x and
 * z, clamped to the target's border, with the nether's short search when going to the nether.
 */
@Mixin(NetherPortalBlock.class)
public abstract class RealmPortalDestinationMixin {
    @Shadow
    private @Nullable TeleportTransition getExitPortal(ServerLevel newLevel, Entity entity, BlockPos portalEntryPos, BlockPos approximateExitPos,
            boolean toNether, WorldBorder worldBorder) {
        throw new AssertionError();
    }

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void apeira$realmPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos,
            CallbackInfoReturnable<@Nullable TeleportTransition> cir) {
        if (!RealmLevels.isRealm(currentLevel.dimension())) {
            return;
        }
        ServerLevel newLevel = RealmLevels.portalTarget(currentLevel);
        if (newLevel == null) {
            cir.setReturnValue(null);
            return;
        }
        boolean toNether = RealmLevels.isRealmNether(newLevel.dimension());
        WorldBorder border = newLevel.getWorldBorder();
        double scale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), newLevel.dimensionType());
        BlockPos approximateExit = border.clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);
        cir.setReturnValue(getExitPortal(newLevel, entity, portalEntryPos, approximateExit, toNether, border));
    }
}
