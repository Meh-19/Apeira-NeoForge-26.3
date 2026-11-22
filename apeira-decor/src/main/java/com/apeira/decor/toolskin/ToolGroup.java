package com.apeira.decor.toolskin;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A kind of tool a {@link ToolSkinItem tool skin} fits, Origin Realms' {@code ToolSkin_ToolGroup}. Each group is the item tag
 * {@code apeira:tool_skin/<group>} (decor's {@code data/apeira/tags/item/tool_skin/}), which takes vanilla's and the
 * common tags ({@code #minecraft:swords}, {@code #c:tools/shield} ...), so every tier of a tool, and any modded tool put in
 * those tags, takes the group's skins with no change here. {@code #apeira:tool_skin/skinnable} holds every group.
 *
 * <p>The fallback glyph is the group's icon in {@link ToolSkinIcons#FONT} for a tool with no glyph of its own (the
 * diamond tool's, the plain shield's, as Origin Realms' {@code default.json} draws them).
 */
public enum ToolGroup {
    SWORD('ꈅ'),
    PICKAXE('ꈆ'),
    AXE('ꈈ'),
    SHOVEL('ꈇ'),
    HOE('ꈉ'),
    SPEAR('ꉛ'),
    SHIELD('ꈳ'),
    BOW('ꈊ'),
    CROSSBOW('ꈌ'),
    TRIDENT('ꈎ'),
    FISHING_ROD('ꈍ'),
    SHEARS('ꈏ');

    private final char glyph;
    private final TagKey<Item> tag;

    ToolGroup(char glyph) {
        this.glyph = glyph;
        this.tag = TagKey.create(Registries.ITEM, ApeiraIds.id("tool_skin/" + id()));
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** {@code #apeira:tool_skin/<group>}: the tools this group's skins fit. */
    public TagKey<Item> tag() {
        return tag;
    }

    /** The group's icon in {@link ToolSkinIcons#FONT}, for tools without one of their own. */
    public char glyph() {
        return glyph;
    }

    /** The first group whose tag holds the stack's item, or {@code null} for an item no skin fits. */
    public static @Nullable ToolGroup of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ToolGroup group : values()) {
            if (stack.is(group.tag)) {
                return group;
            }
        }
        return null;
    }
}
