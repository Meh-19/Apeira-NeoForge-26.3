package com.apeira.uicapture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** An item display's synced item and display context. */
@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAccessor {
    @Invoker("getItemStack")
    ItemStack apeira$getItemStack();

    @Invoker("getItemTransform")
    ItemDisplayContext apeira$getItemTransform();
}
