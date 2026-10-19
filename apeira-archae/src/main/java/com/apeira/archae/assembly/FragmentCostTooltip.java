package com.apeira.archae.assembly;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/** The assembly table screen's tooltip over a recipe: "Cost: N" and the fragment's icon on one line, as Argon drew it. */
public record FragmentCostTooltip(int cost, ItemStack fragment) implements TooltipComponent {}
