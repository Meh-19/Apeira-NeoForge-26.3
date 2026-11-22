package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticRarity;
import com.apeira.decor.toolskin.ToolGroup;
import com.apeira.decor.toolskin.ToolSkinIcons;
import com.apeira.decor.toolskin.ToolSkinItem;
import com.apeira.main.Apeira;
import com.apeira.main.toolskin.SalvagingTableMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Tool skins (decor's {@code toolskin} framework) and the salvaging table's menu. The skins are Origin Realms' skin tokens:
 * the Knight Shield Skin from Ren's captures of the smithing table and salvaging table (common, "Medieval Crate Exclusive" in
 * the captured #738B41), the server pack's {@code knight_shield_token} picture and {@code knight_shield}/
 * {@code _blocking} models ({@code models/item/tool_skin/}, item definition {@code items/tool_skin/knight_shield.json}).
 * A new skin is a {@link #skin} line, its {@code tool_skin.apeira.<skin>} name, its token texture
 * {@code textures/item/tool_skin/<skin>_<group>_token.png} and its item definition {@code items/tool_skin/<skin>_<group>.json}.
 */
public final class MainToolSkins {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredItem<ToolSkinItem>> ALL = new ArrayList<>();
    private static final Component MEDIEVAL_CRATE = CosmeticItem.exclusive("medieval_crate");

    public static final DeferredItem<ToolSkinItem> KNIGHT_SHIELD_SKIN = skin("knight", ToolGroup.SHIELD, CosmeticRarity.COMMON,
            MEDIEVAL_CRATE);

    /** The salvaging table's menu, which takes skins off tools. */
    public static final DeferredHolder<MenuType<?>, MenuType<SalvagingTableMenu>> SALVAGING_TABLE_MENU = R.forRegistry(Registries.MENU)
            .register("salvaging_table", () -> new MenuType<SalvagingTableMenu>(SalvagingTableMenu::new, FeatureFlags.VANILLA_SET));

    static {
        // Origin Realms' default.json glyphs of Apeira's own tools, in apeira-main's font/tool_icons.json.
        icon("platinum_shovel", '\uA213');
        icon("shadow_shovel", '\uA214');
        icon("platinum_axe", '\uA21A');
        icon("shadow_axe", '\uA21B');
        icon("platinum_hoe", '\uA221');
        icon("shadow_hoe", '\uA222');
        icon("platinum_pickaxe", '\uA228');
        icon("shadow_pickaxe", '\uA229');
        icon("platinum_sword", '\uA22F');
        icon("shadow_sword", '\uA230');
    }

    private MainToolSkins() {}

    private static void icon(String item, char glyph) {
        ToolSkinIcons.register(R.id(item), glyph);
    }

    public static void init(ApeiraRegistrar registrar) {}

    /** Registers {@code <skin>_<group>_skin}, Origin Realms' {@code <skin>_<group>_skin_token}. */
    private static DeferredItem<ToolSkinItem> skin(String skin, ToolGroup group, CosmeticRarity rarity, Component exclusive) {
        DeferredItem<ToolSkinItem> item = R.items().registerItem(skin + "_" + group.id() + "_skin",
                p -> new ToolSkinItem(p, skin, group, rarity, exclusive));
        ALL.add(item);
        return item;
    }

    public static List<DeferredItem<ToolSkinItem>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
