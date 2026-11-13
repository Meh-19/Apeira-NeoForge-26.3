package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.Interaction;

/** Reads an interaction entity's synced response flag, which {@code Interaction} keeps private. */
@Mixin(Interaction.class)
public interface InteractionAccessor {
    @Invoker("getResponse")
    boolean apeira$getResponse();
}
