package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.math.Transformation;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;

/** A display entity's synced transformation and billboard mode, read as the server set them (not the interpolated render state). */
@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("createTransformation")
    static Transformation apeira$createTransformation(SynchedEntityData data) {
        throw new AssertionError();
    }

    @Invoker("getBillboardConstraints")
    Display.BillboardConstraints apeira$getBillboardConstraints();
}
