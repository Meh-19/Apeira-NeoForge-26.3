package com.apeira.decor.toolskin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The tool icons a skinned tool's lore starts with ("ꈳ + Knight Skin"), Origin Realms' {@code default.json} glyphs U+A201 to
 * U+A267: each an 8x8 picture of the tool (ascent 8, height 8), drawn white. They are in {@link #FONT},
 * {@code assets/apeira/font/tool_icons.json}, at the server's code points: decor's file has the vanilla tools (their own item
 * textures; the shield's is the server's {@code custom/custom_gear/shield.png}, as {@code textures/gui/tool_skin/shield.png},
 * since vanilla draws the shield as a special model), and a module adds its own tools' glyphs to the same font file in its
 * resources and {@link #register registers} them here (apeira-main does for platinum and shadow tools). The server's glyphs
 * for tools Apeira does not have (legendary, toxic, spirit, gravitite gear) are left out. A tool without a glyph of its own
 * shows its {@link ToolGroup#glyph() group's}.
 */
public final class ToolSkinIcons {
    /** {@code apeira:tool_icons}. */
    public static final FontDescription FONT = new FontDescription.Resource(ApeiraIds.id("tool_icons"));
    private static final Map<Identifier, Character> ICONS = new ConcurrentHashMap<>();

    static {
        icon("minecraft:diamond_sword", 'ꈅ');
        icon("minecraft:diamond_pickaxe", 'ꈆ');
        icon("minecraft:diamond_shovel", 'ꈇ');
        icon("minecraft:diamond_axe", 'ꈈ');
        icon("minecraft:diamond_hoe", 'ꈉ');
        icon("minecraft:bow", 'ꈊ');
        icon("minecraft:crossbow", 'ꈌ');
        icon("minecraft:fishing_rod", 'ꈍ');
        icon("minecraft:trident", 'ꈎ');
        icon("minecraft:shears", 'ꈏ');
        icon("minecraft:iron_shovel", 'ꈑ');
        icon("minecraft:golden_shovel", 'ꈒ');
        icon("minecraft:wooden_axe", 'ꈖ');
        icon("minecraft:stone_axe", 'ꈗ');
        icon("minecraft:iron_axe", 'ꈘ');
        icon("minecraft:golden_axe", 'ꈙ');
        icon("minecraft:wooden_hoe", 'ꈝ');
        icon("minecraft:stone_hoe", 'ꈞ');
        icon("minecraft:iron_hoe", 'ꈟ');
        icon("minecraft:golden_hoe", 'ꈠ');
        icon("minecraft:wooden_pickaxe", 'ꈤ');
        icon("minecraft:stone_pickaxe", 'ꈥ');
        icon("minecraft:iron_pickaxe", 'ꈦ');
        icon("minecraft:golden_pickaxe", 'ꈧ');
        icon("minecraft:wooden_sword", 'ꈫ');
        icon("minecraft:stone_sword", 'ꈬ');
        icon("minecraft:iron_sword", 'ꈭ');
        icon("minecraft:golden_sword", 'ꈮ');
        icon("minecraft:shield", 'ꈳ');
        icon("minecraft:netherite_sword", 'ꈶ');
        icon("minecraft:netherite_shovel", 'ꈷ');
        icon("minecraft:netherite_hoe", 'ꈸ');
        icon("minecraft:netherite_pickaxe", 'ꈹ');
        icon("minecraft:netherite_axe", 'ꈺ');
        icon("minecraft:stone_shovel", 'ꉇ');
        icon("minecraft:copper_pickaxe", 'ꉔ');
        icon("minecraft:copper_sword", 'ꉕ');
        icon("minecraft:copper_shovel", 'ꉖ');
        icon("minecraft:copper_hoe", 'ꉗ');
        icon("minecraft:copper_axe", 'ꉘ');
        icon("minecraft:wooden_shovel", 'ꉙ');
        icon("minecraft:diamond_spear", 'ꉛ');
        icon("minecraft:wooden_spear", 'ꉢ');
        icon("minecraft:stone_spear", 'ꉣ');
        icon("minecraft:copper_spear", 'ꉤ');
        icon("minecraft:iron_spear", 'ꉥ');
        icon("minecraft:golden_spear", 'ꉦ');
        icon("minecraft:netherite_spear", 'ꉧ');
    }

    private ToolSkinIcons() {}

    private static void icon(String item, char glyph) {
        register(Identifier.parse(item), glyph);
    }

    /** Gives {@code item} the glyph {@code glyph} of {@link #FONT}, which the registering module's font file must hold. */
    public static void register(Identifier item, char glyph) {
        ICONS.put(item, glyph);
    }

    /** The stack's tool icon: its own, else its group's, else the sword's. */
    public static char glyph(ItemStack stack) {
        Character own = ICONS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (own != null) {
            return own;
        }
        ToolGroup group = ToolGroup.of(stack);
        return group != null ? group.glyph() : ToolGroup.SWORD.glyph();
    }
}
