package com.apeira.decor.cosmetic;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Where a cosmetic is worn: on the head, on the back, or in the off hand. CosmoLib's three wardrobe slots. */
public enum CosmeticSlot implements StringRepresentable {
    HAT,
    BACK,
    HAND;

    public static final Codec<CosmeticSlot> CODEC = StringRepresentable.fromEnum(CosmeticSlot::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
