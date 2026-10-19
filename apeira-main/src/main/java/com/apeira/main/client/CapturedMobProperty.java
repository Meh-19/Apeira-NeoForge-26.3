package com.apeira.main.client;

import org.jspecify.annotations.Nullable;

import com.apeira.main.item.CapturedMob;
import com.apeira.main.registry.MainDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code apeira:captured_mob} item model property: the type of the mob in a capture net, or none when it is empty.
 * The net's item definition selects on it to show the mob's overlay, like Xenon's custom model data overrides.
 */
public record CapturedMobProperty() implements SelectItemModelProperty<ResourceKey<EntityType<?>>> {
    public static final Codec<ResourceKey<EntityType<?>>> VALUE_CODEC = ResourceKey.codec(Registries.ENTITY_TYPE);
    public static final SelectItemModelProperty.Type<CapturedMobProperty, ResourceKey<EntityType<?>>> TYPE = SelectItemModelProperty.Type
            .create(MapCodec.unit(new CapturedMobProperty()), VALUE_CODEC);

    @Override
    public @Nullable ResourceKey<EntityType<?>> get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
            ItemDisplayContext displayContext) {
        CapturedMob captured = stack.get(MainDataComponents.CAPTURED_MOB);
        return captured == null ? null : BuiltInRegistries.ENTITY_TYPE.getResourceKey(captured.entity().type()).orElse(null);
    }

    @Override
    public SelectItemModelProperty.Type<CapturedMobProperty, ResourceKey<EntityType<?>>> type() {
        return TYPE;
    }

    @Override
    public Codec<ResourceKey<EntityType<?>>> valueCodec() {
        return VALUE_CODEC;
    }
}
