package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.menu.PaintingTableMenu;
import com.apeira.decor.menu.WardrobeMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Menus of the decor module. Their screens are registered in {@link com.apeira.decor.client.ApeiraDecorClient}. */
public final class DecorMenus {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    public static final DeferredHolder<MenuType<?>, MenuType<PaintingTableMenu>> PAINTING_TABLE = R.forRegistry(Registries.MENU)
            .register("painting_table", () -> new MenuType<PaintingTableMenu>(PaintingTableMenu::new, FeatureFlags.VANILLA_SET));
    /** The player's cosmetic wardrobe, opened with the wardrobe key. */
    public static final DeferredHolder<MenuType<?>, MenuType<WardrobeMenu>> WARDROBE = R.forRegistry(Registries.MENU)
            .register("wardrobe", () -> new MenuType<WardrobeMenu>(WardrobeMenu::new, FeatureFlags.VANILLA_SET));

    private DecorMenus() {}

    public static void init(ApeiraRegistrar registrar) {}
}
