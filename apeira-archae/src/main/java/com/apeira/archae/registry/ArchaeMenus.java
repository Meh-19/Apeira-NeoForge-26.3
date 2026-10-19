package com.apeira.archae.registry;

import com.apeira.archae.ApeiraArchae;
import com.apeira.archae.assembly.AssemblyTableMenu;
import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Menus of the archae module. Their screens are registered in {@link com.apeira.archae.client.ApeiraArchaeClient}. */
public final class ArchaeMenus {
    public static final DeferredHolder<MenuType<?>, MenuType<AssemblyTableMenu>> ASSEMBLY_TABLE = ApeiraArchae.REGISTRAR
            .forRegistry(Registries.MENU)
            .register("assembly_table", () -> new MenuType<AssemblyTableMenu>(AssemblyTableMenu::new, FeatureFlags.VANILLA_SET));

    private ArchaeMenus() {}

    public static void init(ApeiraRegistrar registrar) {}
}
