package com.apeira.decor.toolskin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.registry.DecorDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Putting tool skins on and taking them off. A skinned tool keeps the {@code apeira:tool_skin} component
 * ({@link AppliedToolSkin}) and shows the skin's item definition through {@code minecraft:item_model}, so every renderer (GUI,
 * hands, the ground, item frames) draws the skin with no client hook; taking it off restores the tool's components exactly
 * as they were.
 */
public final class ToolSkins {
    /** The grey of the skin line's "+ Knight Skin", as captured. */
    public static final int LORE_COLOR = 0xA7AEBA;

    private ToolSkins() {}

    public static @Nullable AppliedToolSkin get(ItemStack stack) {
        return stack.get(DecorDataComponents.TOOL_SKIN.get());
    }

    public static boolean isSkinned(ItemStack stack) {
        return get(stack) != null;
    }

    /** Whether {@code token} can go on {@code tool}: a skin of the tool's group, on a tool without a skin. */
    public static boolean canApply(ItemStack token, ItemStack tool) {
        return token.getItem() instanceof ToolSkinItem skin && skin.fits(tool) && !isSkinned(tool);
    }

    /** One of {@code tool} wearing {@code token}'s skin, or an empty stack when it doesn't fit. */
    public static ItemStack apply(ItemStack token, ItemStack tool) {
        if (!canApply(token, tool)) {
            return ItemStack.EMPTY;
        }
        ToolSkinItem skin = (ToolSkinItem) token.getItem();
        Optional<Identifier> replaced = tool.getComponentsPatch().keySet().contains(DataComponents.ITEM_MODEL)
                ? Optional.ofNullable(tool.get(DataComponents.ITEM_MODEL))
                : Optional.empty();
        ItemStack skinned = tool.copyWithCount(1);
        skinned.set(DecorDataComponents.TOOL_SKIN.get(), new AppliedToolSkin(ItemStackTemplate.fromNonEmptyStack(token, 1), replaced));
        skinned.set(DataComponents.ITEM_MODEL, skin.skinModel());
        return skinned;
    }

    /** {@code tool} without its skin (a copy, same count), or a copy unchanged if it has none. */
    public static ItemStack strip(ItemStack tool) {
        AppliedToolSkin applied = get(tool);
        if (applied == null) {
            return tool.copy();
        }
        DataComponentPatch patch = tool.getComponentsPatch()
                .forget(type -> type == DataComponents.ITEM_MODEL || type == DecorDataComponents.TOOL_SKIN.get());
        ItemStack clean = new ItemStack(tool.typeHolder(), tool.getCount(), patch);
        applied.replacedModel().ifPresent(model -> clean.set(DataComponents.ITEM_MODEL, model));
        return clean;
    }

    /** The skin item on {@code tool}, as a stack of one, or an empty stack. */
    public static ItemStack token(ItemStack tool) {
        AppliedToolSkin applied = get(tool);
        return applied == null ? ItemStack.EMPTY : applied.tokenStack();
    }

    /**
     * The lore line a skinned tool starts with, as Origin Realms writes it: the tool's icon from {@link ToolSkinIcons#FONT}
     * in white, then " + Knight Skin" in {@link #LORE_COLOR}.
     */
    public static @Nullable Component loreLine(ItemStack tool) {
        AppliedToolSkin applied = get(tool);
        ToolSkinItem skin = applied == null ? null : applied.skin();
        if (skin == null) {
            return null;
        }
        Style grey = Style.EMPTY.withColor(LORE_COLOR).withItalic(false);
        MutableComponent line = Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false));
        line.append(Component.literal(String.valueOf(ToolSkinIcons.glyph(tool))).withStyle(Style.EMPTY.withFont(ToolSkinIcons.FONT)));
        line.append(Component.literal(" + ").withStyle(grey));
        line.append(skin.skinName().withStyle(grey));
        line.append(Component.translatable("tooltip.apeira.tool_skin.skin").withStyle(grey));
        return line;
    }
}
