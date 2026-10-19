package com.apeira.archae.assembly;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An assembly table recipe: {@code cost} of the {@code fragment} make one {@code result}. Loaded from
 * {@code data/<namespace>/assembly/<name>.json} as Argon's were: {@code {"fragment": ..., "result": ..., "cost": ...}}.
 */
public record AssemblyRecipe(Item fragment, Item result, int cost) {
    public static final Codec<AssemblyRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("fragment").forGetter(AssemblyRecipe::fragment),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(AssemblyRecipe::result),
            ExtraCodecs.POSITIVE_INT.fieldOf("cost").forGetter(AssemblyRecipe::cost))
            .apply(instance, AssemblyRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), AssemblyRecipe::fragment,
            ByteBufCodecs.registry(Registries.ITEM), AssemblyRecipe::result,
            ByteBufCodecs.VAR_INT, AssemblyRecipe::cost,
            AssemblyRecipe::new);

    /** A new stack of the result. */
    public ItemStack assemble() {
        return new ItemStack(result);
    }
}
