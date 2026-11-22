package com.apeira.decor.toolskin;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.cosmetic.CosmeticRarity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A tool skin, Origin Realms' skin token ({@code <skin>_<group>_skin_token}): applied to any tool of its {@link ToolGroup} in
 * the smithing table ({@link ToolSkinRecipe}) it changes the tool's look, and the salvaging table takes it off again,
 * whole. Its name is the rarity badge and the name in the rarity's colour (common is the captured #828282), its lore Ren's
 * capture of the Knight Shield Skin: "Apply this skin to any" / "tool in the Smithing Table" in #9FC5F0, a blank line and
 * the exclusive line.
 *
 * <p>The skin's look is the item definition {@link #skinModel()} ({@code apeira:tool_skin/<skin>_<group>},
 * {@code assets/apeira/items/tool_skin/}), which the skinned tool shows through its {@code minecraft:item_model}, so one
 * definition covers every tier of the group. A new skin is a token item and that item definition; a new tool takes skins by
 * being in its group's tag.
 */
public class ToolSkinItem extends Item {
    /** The tooltip colour of the token's description, Origin Realms' light blue. */
    public static final int DESCRIPTION_COLOR = 0x9FC5F0;
    /** A common token's name colour, as captured (the other rarities use their cosmetic colour). */
    public static final int COMMON_NAME_COLOR = 0x828282;

    private final String skin;
    private final ToolGroup group;
    private final CosmeticRarity rarity;
    private final @Nullable Component exclusive;

    /**
     * @param skin      the skin's id ({@code knight}); its name is {@code tool_skin.apeira.<skin>} ("Knight")
     * @param exclusive the crate line ("Medieval Crate Exclusive", coloured), or {@code null}
     */
    public ToolSkinItem(Item.Properties properties, String skin, ToolGroup group, CosmeticRarity rarity,
            @Nullable Component exclusive) {
        super(properties.stacksTo(1));
        this.skin = skin;
        this.group = group;
        this.rarity = rarity;
        this.exclusive = exclusive;
    }

    public String skin() {
        return skin;
    }

    public ToolGroup group() {
        return group;
    }

    public CosmeticRarity rarity() {
        return rarity;
    }

    public @Nullable Component exclusive() {
        return exclusive;
    }

    /** The skin's short name, "Knight". */
    public MutableComponent skinName() {
        return Component.translatable("tool_skin.apeira." + skin);
    }

    /** The item definition a skinned tool shows: {@code apeira:tool_skin/<skin>_<group>}. */
    public Identifier skinModel() {
        return ApeiraIds.id("tool_skin/" + skin + "_" + group.id());
    }

    /** Whether this skin fits {@code tool}: it is in the skin's group. */
    public boolean fits(ItemStack tool) {
        return !tool.isEmpty() && tool.is(group.tag());
    }

    @Override
    public Component getName(ItemStack stack) {
        int colour = rarity == CosmeticRarity.COMMON ? COMMON_NAME_COLOR : rarity.color();
        return Component.empty()
                .append(Component.literal(String.valueOf(rarity.badge())).withStyle(Style.EMPTY.withFont(CosmeticItem.FONT)))
                .append(" ")
                .append(super.getName(stack).copy().withStyle(Style.EMPTY.withColor(colour)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        Style description = Style.EMPTY.withColor(DESCRIPTION_COLOR).withItalic(false);
        builder.accept(Component.translatable("tooltip.apeira.tool_skin.apply").withStyle(description));
        builder.accept(Component.translatable("tooltip.apeira.tool_skin.apply1").withStyle(description));
        if (exclusive != null) {
            builder.accept(Component.empty());
            builder.accept(exclusive);
        }
    }
}
