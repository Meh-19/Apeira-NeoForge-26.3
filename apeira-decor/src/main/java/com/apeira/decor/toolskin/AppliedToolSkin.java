package com.apeira.decor.toolskin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * {@code apeira:tool_skin}: the skin applied to a tool. {@code token} is the skin item as it went into the smithing table
 * (one of it, components and all), given back by the salvaging table; {@code replacedModel} is the tool's own
 * {@code minecraft:item_model} patch that the skin's model replaced, if it had one, put back when the skin comes off.
 */
public record AppliedToolSkin(ItemStackTemplate token, Optional<Identifier> replacedModel) {
    public static final Codec<AppliedToolSkin> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStackTemplate.CODEC.fieldOf("token").forGetter(AppliedToolSkin::token),
            Identifier.CODEC.optionalFieldOf("replaced_model").forGetter(AppliedToolSkin::replacedModel))
            .apply(i, AppliedToolSkin::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AppliedToolSkin> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, AppliedToolSkin::token,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), AppliedToolSkin::replacedModel,
            AppliedToolSkin::new);

    /** The skin item, as a fresh stack of one. */
    public ItemStack tokenStack() {
        return token.create();
    }

    /** The skin item this was, or {@code null} if it is no longer a tool skin (a removed or changed item). */
    public @Nullable ToolSkinItem skin() {
        return token.item().value() instanceof ToolSkinItem skin ? skin : null;
    }
}
