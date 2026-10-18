package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.paint.Paint;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code apeira:paint} item tint source: tints an item model's {@code tintindex} faces with the item's paint, or
 * with {@code default} (white, which leaves the texture as it is) when it is unpainted. Item definitions use it as
 * {@code {"type": "apeira:paint"}}.
 */
public record PaintTintSource(int defaultColor) implements ItemTintSource {
    public static final MapCodec<PaintTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", -1).forGetter(PaintTintSource::defaultColor))
            .apply(instance, PaintTintSource::new));

    /** Unpainted items keep their texture's colours. */
    public PaintTintSource() {
        this(-1);
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        Integer paint = Paint.get(stack);
        return paint != null ? Paint.tint(paint) : defaultColor;
    }

    @Override
    public MapCodec<PaintTintSource> type() {
        return MAP_CODEC;
    }
}
