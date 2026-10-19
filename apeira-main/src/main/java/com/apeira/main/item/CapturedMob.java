package com.apeira.main.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;

/**
 * The mob held by a capture net ({@code apeira:captured_mob}): its saved data, without position, motion and UUID, and
 * the name it had when caught, which the tooltip shows.
 */
public record CapturedMob(TypedEntityData<EntityType<?>> entity, Component name) {
    public static final Codec<CapturedMob> CODEC = RecordCodecBuilder.create(i -> i.group(
            TypedEntityData.codec(EntityType.CODEC).fieldOf("entity").forGetter(CapturedMob::entity),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(CapturedMob::name)).apply(i, CapturedMob::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CapturedMob> STREAM_CODEC = StreamCodec.composite(
            TypedEntityData.streamCodec(EntityType.STREAM_CODEC), CapturedMob::entity, ComponentSerialization.STREAM_CODEC,
            CapturedMob::name, CapturedMob::new);
}
