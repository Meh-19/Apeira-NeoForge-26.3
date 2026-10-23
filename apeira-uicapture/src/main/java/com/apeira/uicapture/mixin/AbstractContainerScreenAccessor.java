package com.apeira.uicapture.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** The label positions a container screen keeps protected, for the capture's container section. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("titleLabelX")
    int apeira$titleLabelX();

    @Accessor("titleLabelY")
    int apeira$titleLabelY();

    @Accessor("inventoryLabelX")
    int apeira$inventoryLabelX();

    @Accessor("inventoryLabelY")
    int apeira$inventoryLabelY();

    @Accessor("hoveredSlot")
    @Nullable Slot apeira$hoveredSlot();
}
